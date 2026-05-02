package com.drc.aidbridge.ui.main.fragment.victim;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.databinding.FragmentVictimSupplyTabBinding;
import com.drc.aidbridge.databinding.ItemSupplyCategoryBinding;
import com.drc.aidbridge.domain.model.victim.VictimSupplyCategory;
import com.drc.aidbridge.domain.model.victim.VictimSupplyItem;
import com.drc.aidbridge.domain.usecase.validation.ValidationResult;
import com.drc.aidbridge.service.UserLocationManager;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.victim.VictimSupplyViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimSupplyTabFragment extends BaseFragment<FragmentVictimSupplyTabBinding> {

    @Inject
    UserLocationManager userLocationManager;

    private VictimSupplyViewModel viewModel;

    private InputMode inputMode = InputMode.MANUAL;
    private boolean isVoiceRecording;
    private boolean isVoicePlaying;
    private AnimatorSet voicePulseAnimator;
    private ActivityResultLauncher<String> recordAudioPermissionLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Double currentLatitude;
    private Double currentLongitude;
    private PendingSubmitInput pendingSubmitInput;
    private PendingVoiceSubmitInput pendingVoiceSubmitInput;
    private MediaRecorder voiceRecorder;
    private MediaPlayer mediaPlayer;
    private File voiceRecordingFile;

    private final List<VictimSupplyCategory> categoryData = new ArrayList<>();
    private final LinkedHashMap<String, Boolean> itemSelections = new LinkedHashMap<>();

    private enum InputMode {
        MANUAL, VOICE
    }

    @Nullable
    @Override
    protected FragmentVictimSupplyTabBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimSupplyTabBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(VictimSupplyViewModel.class);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupInputModeToggle();
        setupSteppers();
        setupSpeechToText();
        setupLocationPermissionLauncher();
        binding.btnSubmitSupply.setOnClickListener(v -> submitByCurrentMode());

        renderCategoriesLoading(true);
        viewModel.loadCategories();
        syncCurrentLocationFromCache();
        userLocationManager.refreshOnce();
        if (userLocationManager.hasLocationPermission()) {
            fetchCurrentLocation(false);
        }
    }

    @Override
    protected void observeViewModel() {
        viewModel.getValidationError().observe(getViewLifecycleOwner(), this::renderValidationError);

        viewModel.getCategoriesResult().observe(getViewLifecycleOwner(), this::handleCategoryState);

        viewModel.getSubmitResult().observe(
            getViewLifecycleOwner(),
            resultObserver(this::handleSubmitSuccess, this::handleSubmitError)
        );

        viewModel.getVoiceSubmitResult().observe(
            getViewLifecycleOwner(),
            resultObserver(this::handleSubmitSuccess, this::handleSubmitError)
        );
    }

    private void renderValidationError(@Nullable ValidationResult validation) {
        if (validation == null || validation.isValid()) {
            return;
        }

        String message = validation.getErrorMessage();
        showTopSnackbar(
            binding.mainContainer,
            message != null && !message.trim().isEmpty()
                ? message
                : getString(R.string.victim_supply_submit_error),
            true
        );
    }

    @Override
    protected void onLoadingStateChanged(boolean isLoading) {
        binding.btnSubmitSupply.setEnabled(!isLoading);
        binding.progressSubmitSupply.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnSubmitSupply.setText(isLoading ? R.string.btn_loading : R.string.victim_supply_submit);
    }

    private void handleCategoryState(NetworkResultWrapper<List<VictimSupplyCategory>> result) {
        if (result == null) {
            return;
        }

        if (result.isLoading()) {
            renderCategoriesLoading(true);
            return;
        }

        renderCategoriesLoading(false);

        if (result.isError()) {
            if (!result.hasBeenHandled()) {
                result.markAsHandled();
                String message = result.getMessage();
                showTopSnackbar(
                    binding.mainContainer,
                    message != null && !message.trim().isEmpty()
                        ? message
                        : getString(R.string.victim_supply_load_categories_error),
                    true
                );
            }

            renderEmptyState(true);
            return;
        }

        List<VictimSupplyCategory> categories = result.getData();
        categoryData.clear();
        if (categories != null) {
            categoryData.addAll(categories);
        }

        if (categoryData.isEmpty()) {
            renderEmptyState(true);
            return;
        }

        renderEmptyState(false);
        setupDynamicCategories(categoryData);
    }

    private void setupDynamicCategories(List<VictimSupplyCategory> categories) {
        binding.containerCategories.removeAllViews();
        Set<String> validItemIds = new LinkedHashSet<>();

        int index = 0;
        for (VictimSupplyCategory category : categories) {
            if (category == null) {
                continue;
            }

            ItemSupplyCategoryBinding categoryBinding = ItemSupplyCategoryBinding.inflate(
                getLayoutInflater(),
                binding.containerCategories,
                false
            );

            String categoryName = safeText(category.getName());
            categoryBinding.tvCategoryName.setText(categoryName);
            categoryBinding.layoutHeader.setOnClickListener(v -> toggleCategory(categoryBinding));

            List<VictimSupplyItem> items = category.getItems() != null ? category.getItems() : new ArrayList<>();
            for (VictimSupplyItem item : items) {
                if (item == null) {
                    continue;
                }

                String itemId = safeText(item.getId());
                if (itemId.isEmpty()) {
                    continue;
                }

                validItemIds.add(itemId);
                if (!itemSelections.containsKey(itemId)) {
                    itemSelections.put(itemId, false);
                }

                categoryBinding.layoutContent.addView(createItemSelectionRow(categoryBinding, items, item));
            }

            if (items.isEmpty()) {
                categoryBinding.layoutContent.addView(createEmptyCategoryItemView());
            }

            updateSelectedCount(categoryBinding, items);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            if (index > 0) {
                params.topMargin = getResources().getDimensionPixelSize(R.dimen.victim_supply_category_item_margin_top);
            }
            binding.containerCategories.addView(categoryBinding.getRoot(), params);
            index++;
        }

        itemSelections.keySet().retainAll(validItemIds);
    }

    private View createItemSelectionRow(ItemSupplyCategoryBinding categoryBinding,
                                        List<VictimSupplyItem> categoryItems,
                                        VictimSupplyItem item) {
        LinearLayout container = new LinearLayout(requireContext());
        container.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, getResources().getDimensionPixelSize(R.dimen.spacing_xs), 0, 0);

        String itemId = safeText(item.getId());
        MaterialCheckBox checkBox = new MaterialCheckBox(requireContext());
        checkBox.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        checkBox.setText(safeText(item.getName()));
        checkBox.setChecked(isItemSelected(itemId));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setItemSelected(itemId, isChecked);
            updateSelectedCount(categoryBinding, categoryItems);
        });

        container.addView(checkBox);
        return container;
    }

    private View createEmptyCategoryItemView() {
        TextView textView = new TextView(requireContext());
        textView.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        textView.setPadding(0, getResources().getDimensionPixelSize(R.dimen.spacing_xs), 0, 0);
        textView.setText(R.string.victim_supply_level2_empty);
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        textView.setTextSize(14);
        return textView;
    }

    private void toggleCategory(ItemSupplyCategoryBinding itemBinding) {
        boolean isExpanded = itemBinding.layoutContent.getVisibility() == View.VISIBLE;
        itemBinding.layoutContent.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
        float targetRotation = isExpanded ? 0f : 180f;
        itemBinding.ivArrow.animate().rotation(targetRotation).setDuration(180).start();

        int contentDescRes = isExpanded
            ? R.string.victim_supply_list_expand_desc
            : R.string.victim_supply_list_collapse_desc;
        itemBinding.ivArrow.setContentDescription(getString(contentDescRes));
    }

    private void updateSelectedCount(ItemSupplyCategoryBinding itemBinding, List<VictimSupplyItem> categoryItems) {
        int selectedCount = 0;
        for (VictimSupplyItem item : categoryItems) {
            if (item == null) {
                continue;
            }

            String itemId = safeText(item.getId());
            if (itemId.isEmpty()) {
                continue;
            }

            if (isItemSelected(itemId)) {
                selectedCount++;
            }
        }

        itemBinding.tvSelectedCount.setText(getString(R.string.victim_supply_selected_count_format, selectedCount));
    }

    private boolean isItemSelected(String itemId) {
        return Boolean.TRUE.equals(itemSelections.get(itemId));
    }

    private void setItemSelected(String itemId, boolean isSelected) {
        itemSelections.put(itemId, isSelected);
    }

    private void renderCategoriesLoading(boolean isLoading) {
        binding.layoutCategoriesLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            binding.containerCategories.setVisibility(View.GONE);
            binding.tvEmptyCategories.setVisibility(View.GONE);
        }
    }

    private void renderEmptyState(boolean isEmpty) {
        binding.tvEmptyCategories.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.containerCategories.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }

    private void setupInputModeToggle() {
        binding.toggleInputMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }

            // Sử dụng TransitionManager để tạo hiệu ứng chuyển cảnh mượt mà
            AutoTransition transition = new AutoTransition();
            transition.setDuration(250);
            TransitionManager.beginDelayedTransition(binding.mainContainer, transition);

            if (checkedId == binding.btnInputVoice.getId()) {
                setInputMode(InputMode.VOICE);
            } else {
                setInputMode(InputMode.MANUAL);
            }
        });

        binding.toggleInputMode.check(binding.btnInputManual.getId());
        setInputMode(InputMode.MANUAL);
    }

    private void setInputMode(InputMode mode) {
        if (mode == null) {
            return;
        }

        if (inputMode == mode) {
            updateUiForInputMode();
            return;
        }

        inputMode = mode;
        if (inputMode == InputMode.MANUAL) {
            stopVoiceRecording();
            stopVoicePlayback();
            clearVoiceRecordingFile();
        }

        updateUiForInputMode();
    }

    private void updateUiForInputMode() {
        boolean isManual = inputMode == InputMode.MANUAL;
        binding.layoutManualSection.setVisibility(isManual ? View.VISIBLE : View.GONE);
        binding.layoutVoiceSection.setVisibility(isManual ? View.GONE : View.VISIBLE);
        updateVoiceStatusText();
    }

    private void setupSteppers() {
        setupStepper(binding.btnMinusAdult, binding.btnPlusAdult, binding.tvCountAdult);
        setupStepper(binding.btnMinusElderly, binding.btnPlusElderly, binding.tvCountElderly);
        setupStepper(binding.btnMinusChild, binding.btnPlusChild, binding.tvCountChild);
    }

    private void setupStepper(View minusView, View plusView, TextView countView) {
        minusView.setOnClickListener(v -> updateCount(countView, -1));
        plusView.setOnClickListener(v -> updateCount(countView, 1));
    }

    private void updateCount(TextView countView, int delta) {
        String currentText = String.valueOf(countView.getText()).trim();
        int currentCount = currentText.isEmpty() ? 0 : parseInt(currentText);
        int nextCount = Math.max(0, currentCount + delta);
        countView.setText(String.valueOf(nextCount));
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void submitByCurrentMode() {
        if (inputMode == InputMode.VOICE) {
            submitVoiceRequest();
        } else {
            submitManualRequest();
        }
    }

    private void submitManualRequest() {
        List<VictimSupplyViewModel.RequestedItem> items = collectRequestedItems();
        String notes = safeText(binding.etNotes.getText() != null ? binding.etNotes.getText().toString() : "");
        int adults = parseInt(binding.tvCountAdult.getText().toString());
        int elderly = parseInt(binding.tvCountElderly.getText().toString());
        int children = parseInt(binding.tvCountChild.getText().toString());

        pendingSubmitInput = new PendingSubmitInput(items, adults, elderly, children, notes);
        checkLocationAndSubmit();
    }

    private void submitVoiceRequest() {
        if (voiceRecordingFile == null || !voiceRecordingFile.exists()) {
            showTopSnackbar(binding.mainContainer, getString(R.string.victim_supply_voice_empty_error), true);
            return;
        }

        int adults = parseInt(binding.tvCountAdult.getText().toString());
        int elderly = parseInt(binding.tvCountElderly.getText().toString());
        int children = parseInt(binding.tvCountChild.getText().toString());

        pendingVoiceSubmitInput = new PendingVoiceSubmitInput(voiceRecordingFile, adults, elderly, children);
        checkLocationAndSubmit();
    }

    private List<VictimSupplyViewModel.RequestedItem> collectRequestedItems() {
        List<VictimSupplyViewModel.RequestedItem> requestedItems = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : itemSelections.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                requestedItems.add(new VictimSupplyViewModel.RequestedItem(entry.getKey(), 1));
            }
        }
        return requestedItems;
    }

    private void checkLocationAndSubmit() {
        if (userLocationManager.hasLocationPermission()) {
            fetchCurrentLocation(true);
        } else {
            locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void setupLocationPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean fineLocation = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                Boolean coarseLocation = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                if ((fineLocation != null && fineLocation) || (coarseLocation != null && coarseLocation)) {
                    fetchCurrentLocation(true);
                } else {
                    showTopSnackbar(binding.mainContainer, getString(R.string.victim_supply_permission_location_denied), true);
                }
            }
        );
    }

    private void fetchCurrentLocation(boolean shouldSubmitAfterFetch) {
        if (!userLocationManager.isLocationProviderEnabled()) {
            if (shouldSubmitAfterFetch) {
                showTopSnackbar(binding.mainContainer, getString(R.string.victim_supply_location_disabled), true);
            }
            return;
        }

        try {
            fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        if (shouldSubmitAfterFetch) {
                            executeSubmitWithLocation();
                        }
                    } else if (shouldSubmitAfterFetch) {
                        showTopSnackbar(binding.mainContainer, getString(R.string.victim_supply_location_unavailable), true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (shouldSubmitAfterFetch) {
                        showTopSnackbar(binding.mainContainer, getString(R.string.victim_supply_location_unavailable), true);
                    }
                });
        } catch (SecurityException e) {
            if (shouldSubmitAfterFetch) {
                showTopSnackbar(binding.mainContainer, getString(R.string.victim_supply_permission_location_denied), true);
            }
        }
    }

    private void executeSubmitWithLocation() {
        if (currentLatitude == null || currentLongitude == null) {
            showTopSnackbar(binding.mainContainer, getString(R.string.victim_supply_location_unavailable), true);
            return;
        }

        if (inputMode == InputMode.VOICE && pendingVoiceSubmitInput != null) {
            viewModel.submitVoiceRequest(
                pendingVoiceSubmitInput.adults,
                pendingVoiceSubmitInput.elderly,
                pendingVoiceSubmitInput.children,
                "",
                currentLatitude,
                currentLongitude,
                pendingVoiceSubmitInput.file
            );
            pendingVoiceSubmitInput = null;
        } else if (pendingSubmitInput != null) {
            viewModel.submitRequest(
                pendingSubmitInput.adults,
                pendingSubmitInput.elderly,
                pendingSubmitInput.children,
                pendingSubmitInput.notes,
                pendingSubmitInput.items,
                currentLatitude,
                currentLongitude
            );
            pendingSubmitInput = null;
        }
    }

    private void syncCurrentLocationFromCache() {
        UserLocationManager.LocationSnapshot lastLocation = userLocationManager.getLatestLocation();
        if (lastLocation != null) {
            currentLatitude = lastLocation.getLatitude();
            currentLongitude = lastLocation.getLongitude();
        }
    }

    private void setupSpeechToText() {
        recordAudioPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    startVoiceRecording();
                } else {
                    showTopSnackbar(binding.mainContainer, getString(R.string.victim_supply_voice_permission_denied), true);
                }
            }
        );

        binding.btnVoiceToggle.setOnClickListener(v -> {
            if (isVoiceRecording) {
                stopVoiceRecording();
            } else {
                checkVoicePermissionAndStart();
            }
        });

        binding.btnVoicePlay.setOnClickListener(v -> {
            if (isVoicePlaying) {
                stopVoicePlayback();
            } else {
                startVoicePlayback();
            }
        });
    }

    private void checkVoicePermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecording();
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startVoiceRecording() {
        stopVoicePlayback();
        isVoiceRecording = true;
        updateVoiceStatusText();
        startVoicePulseAnimation();

        try {
            voiceRecordingFile = File.createTempFile("victim_supply_voice_", ".m4a", requireContext().getCacheDir());
            voiceRecorder = new MediaRecorder();
            voiceRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            voiceRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            voiceRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            voiceRecorder.setOutputFile(voiceRecordingFile.getAbsolutePath());
            voiceRecorder.prepare();
            voiceRecorder.start();
        } catch (IOException e) {
            stopVoiceRecording();
            showTopSnackbar(binding.mainContainer, getString(R.string.victim_supply_voice_init_error), true);
        }
    }

    private void stopVoiceRecording() {
        if (!isVoiceRecording) return;
        isVoiceRecording = false;
        updateVoiceStatusText();
        stopVoicePulseAnimation();

        if (voiceRecorder != null) {
            try {
                voiceRecorder.stop();
            } catch (RuntimeException e) {
                // Ignore stop failure
            }
            voiceRecorder.release();
            voiceRecorder = null;
        }
    }

    private void startVoicePlayback() {
        if (voiceRecordingFile == null || !voiceRecordingFile.exists()) return;

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(voiceRecordingFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(mp -> stopVoicePlayback());
            mediaPlayer.start();
            isVoicePlaying = true;
            updateVoiceStatusText();
        } catch (IOException e) {
            showTopSnackbar(binding.mainContainer, getString(R.string.error_generic), true);
        }
    }

    private void stopVoicePlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isVoicePlaying = false;
        updateVoiceStatusText();
    }

    private void clearVoiceRecordingFile() {
        if (voiceRecordingFile != null && voiceRecordingFile.exists()) {
            voiceRecordingFile.delete();
        }
        voiceRecordingFile = null;
    }

    private void updateVoiceStatusText() {
        if (inputMode == InputMode.MANUAL) return;

        if (isVoiceRecording) {
            binding.tvVoiceStatus.setText(R.string.victim_supply_voice_recording);
            binding.btnVoiceToggle.setIconResource(R.drawable.ic_stop);
            binding.btnVoiceToggle.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.sos_red)));
            binding.btnVoicePlay.setVisibility(View.GONE);
        } else {
            if (voiceRecordingFile != null && voiceRecordingFile.exists()) {
                binding.tvVoiceStatus.setText(R.string.victim_supply_voice_recorded);
                binding.btnVoiceToggle.setIconResource(R.drawable.ic_mic);
                binding.btnVoiceToggle.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.safe_green)));
                
                binding.btnVoicePlay.setVisibility(View.VISIBLE);
                binding.btnVoicePlay.setIconResource(isVoicePlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            } else {
                binding.tvVoiceStatus.setText(R.string.victim_supply_voice_idle);
                binding.btnVoiceToggle.setIconResource(R.drawable.ic_mic);
                binding.btnVoiceToggle.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.color_primary)));
                binding.btnVoicePlay.setVisibility(View.GONE);
            }
        }
    }

    private void startVoicePulseAnimation() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(binding.btnVoiceToggle, View.SCALE_X, 1f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(binding.btnVoiceToggle, View.SCALE_Y, 1f, 1.2f, 1f);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);

        voicePulseAnimator = new AnimatorSet();
        voicePulseAnimator.playTogether(scaleX, scaleY);
        voicePulseAnimator.setDuration(1000);
        voicePulseAnimator.start();
    }

    private void stopVoicePulseAnimation() {
        if (voicePulseAnimator != null) {
            voicePulseAnimator.cancel();
            binding.btnVoiceToggle.setScaleX(1f);
            binding.btnVoiceToggle.setScaleY(1f);
        }
    }

    private void handleSubmitSuccess(String message) {
        showTopSnackbar(binding.mainContainer, message != null ? message : getString(R.string.victim_supply_submit_success), false);
        resetForm();
    }

    private void handleSubmitError(String message) {
        showTopSnackbar(binding.mainContainer, message != null ? message : getString(R.string.victim_supply_submit_error), true);
    }

    private void resetForm() {
        itemSelections.clear();
        setupDynamicCategories(categoryData);
        binding.etNotes.setText("");
        binding.tvCountAdult.setText(R.string.victim_supply_count_default);
        binding.tvCountElderly.setText(R.string.victim_supply_count_default);
        binding.tvCountChild.setText(R.string.victim_supply_count_default);
        clearVoiceRecordingFile();
        stopVoicePlayback();
        updateVoiceStatusText();
    }

    @Override
    public void onDestroyView() {
        stopVoicePlayback();
        super.onDestroyView();
    }

    private static class PendingSubmitInput {
        final List<VictimSupplyViewModel.RequestedItem> items;
        final int adults;
        final int elderly;
        final int children;
        final String notes;

        PendingSubmitInput(List<VictimSupplyViewModel.RequestedItem> items, int adults, int elderly, int children, String notes) {
            this.items = items;
            this.adults = adults;
            this.elderly = elderly;
            this.children = children;
            this.notes = notes;
        }
    }

    private static class PendingVoiceSubmitInput {
        final File file;
        final int adults;
        final int elderly;
        final int children;

        PendingVoiceSubmitInput(File file, int adults, int elderly, int children) {
            this.file = file;
            this.adults = adults;
            this.elderly = elderly;
            this.children = children;
        }
    }
}
