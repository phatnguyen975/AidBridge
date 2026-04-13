package com.drc.aidbridge.ui.main.fragment.victim;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.drc.aidbridge.ui.base.BaseFragment;
import com.drc.aidbridge.ui.main.viewmodel.victim.VictimSupplyViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimSupplyTabFragment extends BaseFragment<FragmentVictimSupplyTabBinding> {

    private VictimSupplyViewModel viewModel;

    private boolean isRecording;
    private AnimatorSet voicePulseAnimator;
    private ActivityResultLauncher<String> recordAudioPermissionLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Double currentLatitude;
    private Double currentLongitude;
    private PendingSubmitInput pendingSubmitInput;

    private final List<VictimSupplyCategory> categoryData = new ArrayList<>();
    private final LinkedHashMap<String, Boolean> itemSelections = new LinkedHashMap<>();

    @Nullable
    @Override
    protected FragmentVictimSupplyTabBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimSupplyTabBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(VictimSupplyViewModel.class);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupSteppers();
        setupSpeechToText();
        setupLocationPermissionLauncher();
        binding.btnSubmitSupply.setOnClickListener(v -> extractDataAndSubmit());

        renderCategoriesLoading(true);
        viewModel.loadCategories();
        fetchCurrentLocation(false);
    }

    @Override
    protected void observeViewModel() {
        viewModel.getValidationError().observe(getViewLifecycleOwner(), this::renderValidationError);

        viewModel.getCategoriesResult().observe(getViewLifecycleOwner(), this::handleCategoryState);

        viewModel.getSubmitResult().observe(
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
            binding.getRoot(),
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
                    binding.getRoot(),
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

    private List<VictimSupplyViewModel.RequestedItem> collectRequestedItems() {
        List<VictimSupplyViewModel.RequestedItem> requestedItems = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : itemSelections.entrySet()) {
            String itemId = entry.getKey();
            Boolean selected = entry.getValue();

            if (itemId == null || itemId.trim().isEmpty() || !Boolean.TRUE.equals(selected)) {
                continue;
            }

            requestedItems.add(new VictimSupplyViewModel.RequestedItem(itemId.trim(), 1));
        }

        return requestedItems;
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

    private void setupSpeechToText() {
        recordAudioPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (Boolean.TRUE.equals(isGranted)) {
                    launchSpeechRecognizer();
                    return;
                }

                showTopSnackbar(binding.getRoot(), getString(R.string.victim_supply_voice_permission_denied), true);
            }
        );

        speechRecognizerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            this::handleSpeechRecognizerResult
        );

        binding.btnVoiceToggle.setOnClickListener(v -> requestSpeechToText());
    }

    private void requestSpeechToText() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            launchSpeechRecognizer();
            return;
        }

        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
    }

    private void launchSpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.victim_supply_voice_prompt));

        try {
            setRecordingUi(true);
            speechRecognizerLauncher.launch(intent);
        } catch (ActivityNotFoundException exception) {
            setRecordingUi(false);
            showTopSnackbar(binding.getRoot(), getString(R.string.victim_supply_voice_not_supported), true);
        }
    }

    private void handleSpeechRecognizerResult(ActivityResult activityResult) {
        setRecordingUi(false);

        if (activityResult.getResultCode() != Activity.RESULT_OK) {
            showTopSnackbar(binding.getRoot(), getString(R.string.victim_supply_voice_result_error), true);
            return;
        }

        Intent data = activityResult.getData();
        if (data == null) {
            showTopSnackbar(binding.getRoot(), getString(R.string.victim_supply_voice_result_error), true);
            return;
        }

        ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (results == null || results.isEmpty()) {
            showTopSnackbar(binding.getRoot(), getString(R.string.victim_supply_voice_result_error), true);
            return;
        }

        String recognizedText = safeText(results.get(0));
        if (recognizedText.isEmpty()) {
            showTopSnackbar(binding.getRoot(), getString(R.string.victim_supply_voice_result_error), true);
            return;
        }

        appendRecognizedText(recognizedText);
    }

    private void appendRecognizedText(String text) {
        String currentNote = binding.etNotes.getText() != null
            ? binding.etNotes.getText().toString().trim()
            : "";

        String mergedText = currentNote.isEmpty()
            ? text
            : String.format(Locale.getDefault(), "%s\n%s", currentNote, text);

        binding.etNotes.setText(mergedText);
        binding.etNotes.setSelection(mergedText.length());
    }

    private void setRecordingUi(boolean recording) {
        isRecording = recording;
        binding.btnVoiceToggle.setIconResource(recording ? R.drawable.ic_pause : R.drawable.ic_mic);
        binding.tvVoiceStatus.setText(recording ? R.string.victim_supply_voice_recording : R.string.victim_supply_voice_idle);

        if (recording) {
            startVoicePulse();
        } else {
            stopVoicePulse();
        }
    }

    private void startVoicePulse() {
        if (!isRecording) {
            return;
        }

        if (voicePulseAnimator == null) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(binding.btnVoiceToggle, View.SCALE_X, 1.0f, 1.08f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(binding.btnVoiceToggle, View.SCALE_Y, 1.0f, 1.08f);
            scaleX.setRepeatMode(ObjectAnimator.REVERSE);
            scaleY.setRepeatMode(ObjectAnimator.REVERSE);
            scaleX.setRepeatCount(ObjectAnimator.INFINITE);
            scaleY.setRepeatCount(ObjectAnimator.INFINITE);
            scaleX.setDuration(700L);
            scaleY.setDuration(700L);

            voicePulseAnimator = new AnimatorSet();
            voicePulseAnimator.playTogether(scaleX, scaleY);
        }

        if (!voicePulseAnimator.isRunning()) {
            voicePulseAnimator.start();
        }
    }

    private void stopVoicePulse() {
        if (voicePulseAnimator != null) {
            voicePulseAnimator.cancel();
        }
        binding.btnVoiceToggle.setScaleX(1.0f);
        binding.btnVoiceToggle.setScaleY(1.0f);
    }

    private void extractDataAndSubmit() {
        List<VictimSupplyViewModel.RequestedItem> requestedItems = collectRequestedItems();
        int adultCount = parseInt(String.valueOf(binding.tvCountAdult.getText()).trim());
        int elderlyCount = parseInt(String.valueOf(binding.tvCountElderly.getText()).trim());
        int childCount = parseInt(String.valueOf(binding.tvCountChild.getText()).trim());
        String notes = String.valueOf(binding.etNotes.getText()).trim();

        if (requestedItems.isEmpty()) {
            showTopSnackbar(
                binding.getRoot(),
                getString(R.string.victim_supply_validation_items_required),
                true
            );
            return;
        }

        if ((adultCount + elderlyCount + childCount) <= 0) {
            showTopSnackbar(
                binding.getRoot(),
                getString(R.string.victim_supply_validation_people_required),
                true
            );
            return;
        }

        PendingSubmitInput input = new PendingSubmitInput(
            adultCount,
            elderlyCount,
            childCount,
            notes,
            requestedItems
        );

        submitOrFetchLocation(input);
    }

    private void submitOrFetchLocation(PendingSubmitInput input) {
        if (input == null) {
            return;
        }

        if (currentLatitude != null && currentLongitude != null) {
            submitToViewModel(input, currentLatitude, currentLongitude);
            return;
        }

        pendingSubmitInput = input;
        fetchCurrentLocation(true);
    }

    private void submitToViewModel(PendingSubmitInput input, Double latitude, Double longitude) {
        if (input == null) {
            return;
        }

        viewModel.submitRequest(
            input.adultCount,
            input.elderlyCount,
            input.childCount,
            input.notes,
            input.requestedItems,
            latitude,
            longitude
        );
        pendingSubmitInput = null;
    }

    private void setupLocationPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                if (isLocationPermissionGranted()) {
                    fetchCurrentLocation(pendingSubmitInput != null);
                    return;
                }

                pendingSubmitInput = null;
                showTopSnackbar(binding.getRoot(), getString(R.string.victim_supply_permission_location_denied), true);
            }
        );
    }

    private void fetchCurrentLocation(boolean trySubmitAfterFetch) {
        if (!isAdded()) {
            return;
        }

        if (!isLocationPermissionGranted()) {
            locationPermissionLauncher.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }

        if (!isLocationProviderEnabled()) {
            if (trySubmitAfterFetch) {
                pendingSubmitInput = null;
            }
            showTopSnackbar(binding.getRoot(), getString(R.string.victim_supply_location_disabled), true);
            return;
        }

        try {
            fusedLocationProviderClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();

                        if (trySubmitAfterFetch && pendingSubmitInput != null) {
                            submitToViewModel(pendingSubmitInput, currentLatitude, currentLongitude);
                        }
                        return;
                    }

                    requestLastKnownLocation(trySubmitAfterFetch);
                })
                .addOnFailureListener(ignored -> requestLastKnownLocation(trySubmitAfterFetch));
        } catch (SecurityException exception) {
            if (trySubmitAfterFetch) {
                pendingSubmitInput = null;
            }
            showTopSnackbar(binding.getRoot(), getString(R.string.victim_supply_permission_location_denied), true);
        }
    }

    private void requestLastKnownLocation(boolean trySubmitAfterFetch) {
        try {
            fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();

                        if (trySubmitAfterFetch && pendingSubmitInput != null) {
                            submitToViewModel(pendingSubmitInput, currentLatitude, currentLongitude);
                        }
                        return;
                    }

                    if (trySubmitAfterFetch) {
                        pendingSubmitInput = null;
                    }
                    showTopSnackbar(binding.getRoot(), getString(R.string.victim_supply_location_unavailable), true);
                })
                .addOnFailureListener(ignored -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (trySubmitAfterFetch) {
                        pendingSubmitInput = null;
                    }
                    showTopSnackbar(binding.getRoot(), getString(R.string.victim_supply_location_unavailable), true);
                });
        } catch (SecurityException exception) {
            if (trySubmitAfterFetch) {
                pendingSubmitInput = null;
            }
            showTopSnackbar(binding.getRoot(), getString(R.string.victim_supply_permission_location_denied), true);
        }
    }

    private boolean isLocationPermissionGranted() {
        Context context = getContext();
        if (context == null) {
            return false;
        }

        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isLocationProviderEnabled() {
        LocationManager locationManager =
            (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private int parseInt(String value) {
        if (value.isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void handleSubmitSuccess(@Nullable String message) {
        String successMessage = message != null && !message.trim().isEmpty()
            ? message.trim()
            : getString(R.string.victim_supply_submit_success);

        showTopSnackbar(binding.getRoot(), successMessage, false);
        clearInputFocusAndHideKeyboard();
        resetFormAfterSubmit();
    }

    private void handleSubmitError(String message) {
        String errorMessage = message != null && !message.trim().isEmpty()
            ? message.trim()
            : getString(R.string.victim_supply_submit_error);

        showTopSnackbar(binding.getRoot(), errorMessage, true);
    }

    private void resetFormAfterSubmit() {
        binding.etNotes.setText(null);
        binding.tvCountAdult.setText(R.string.victim_supply_count_default);
        binding.tvCountElderly.setText(R.string.victim_supply_count_default);
        binding.tvCountChild.setText(R.string.victim_supply_count_default);

        for (String itemId : new ArrayList<>(itemSelections.keySet())) {
            itemSelections.put(itemId, false);
        }

        if (!categoryData.isEmpty()) {
            setupDynamicCategories(categoryData);
        }
    }

    @Override
    public void onDestroyView() {
        stopVoicePulse();
        voicePulseAnimator = null;
        super.onDestroyView();
    }

    private static final class PendingSubmitInput {
        private final int adultCount;
        private final int elderlyCount;
        private final int childCount;
        private final String notes;
        private final List<VictimSupplyViewModel.RequestedItem> requestedItems;

        private PendingSubmitInput(int adultCount,
                                   int elderlyCount,
                                   int childCount,
                                   String notes,
                                   List<VictimSupplyViewModel.RequestedItem> requestedItems) {
            this.adultCount = Math.max(0, adultCount);
            this.elderlyCount = Math.max(0, elderlyCount);
            this.childCount = Math.max(0, childCount);
            this.notes = notes != null ? notes.trim() : "";
            this.requestedItems = requestedItems != null ? requestedItems : new ArrayList<>();
        }
    }
}
