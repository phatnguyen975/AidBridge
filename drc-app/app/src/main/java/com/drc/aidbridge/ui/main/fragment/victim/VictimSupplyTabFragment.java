package com.drc.aidbridge.ui.main.fragment.victim;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.speech.RecognizerIntent;
import android.util.TypedValue;
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
import com.google.android.material.button.MaterialButton;

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
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;

    private final List<VictimSupplyCategory> categoryData = new ArrayList<>();
    private final LinkedHashMap<String, Integer> itemQuantities = new LinkedHashMap<>();

    @Nullable
    @Override
    protected FragmentVictimSupplyTabBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimSupplyTabBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(VictimSupplyViewModel.class);

        setupSteppers();
        setupSpeechToText();
        binding.btnSubmitSupply.setOnClickListener(v -> extractDataAndSubmit());

        renderCategoriesLoading(true);
        viewModel.loadCategories();
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
                if (!itemQuantities.containsKey(itemId)) {
                    itemQuantities.put(itemId, 0);
                }

                categoryBinding.layoutContent.addView(createItemQuantityRow(categoryBinding, items, item));
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

        itemQuantities.keySet().retainAll(validItemIds);
    }

    private View createItemQuantityRow(ItemSupplyCategoryBinding categoryBinding,
                                       List<VictimSupplyItem> categoryItems,
                                       VictimSupplyItem item) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, getResources().getDimensionPixelSize(R.dimen.spacing_xs), 0, 0);

        TextView tvItemName = new TextView(requireContext());
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvItemName.setLayoutParams(nameParams);
        tvItemName.setText(safeText(item.getName()));
        tvItemName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        tvItemName.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_sm));

        String itemId = safeText(item.getId());
        int initialCount = getItemQuantity(itemId);

        MaterialButton btnMinus = createStepperButton(
            R.drawable.ic_minus,
            R.color.border_default,
            R.string.victim_supply_stepper_decrease_desc
        );

        MaterialButton btnPlus = createStepperButton(
            R.drawable.ic_plus,
            R.color.color_primary,
            R.string.victim_supply_stepper_increase_desc
        );

        TextView tvCount = new TextView(requireContext());
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int horizontalMargin = getResources().getDimensionPixelSize(R.dimen.spacing_md);
        countParams.setMarginStart(horizontalMargin);
        countParams.setMarginEnd(horizontalMargin);
        tvCount.setLayoutParams(countParams);
        tvCount.setText(String.valueOf(initialCount));
        tvCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        tvCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_md));
        tvCount.setTypeface(tvCount.getTypeface(), Typeface.BOLD);

        btnMinus.setOnClickListener(v -> {
            updateItemQuantity(itemId, -1, tvCount);
            updateSelectedCount(categoryBinding, categoryItems);
        });

        btnPlus.setOnClickListener(v -> {
            updateItemQuantity(itemId, 1, tvCount);
            updateSelectedCount(categoryBinding, categoryItems);
        });

        row.addView(tvItemName);
        row.addView(btnMinus);
        row.addView(tvCount);
        row.addView(btnPlus);
        return row;
    }

    private MaterialButton createStepperButton(int iconRes,
                                               int backgroundColorRes,
                                               int contentDescRes) {
        MaterialButton button = new MaterialButton(
            requireContext(),
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        );

        int size = getResources().getDimensionPixelSize(R.dimen.victim_supply_stepper_button_size);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        button.setLayoutParams(params);
        button.setIconResource(iconRes);
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        button.setIconPadding(0);
        button.setContentDescription(getString(contentDescRes));
        button.setBackgroundTintList(ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), backgroundColorRes)
        ));
        button.setCornerRadius(getResources().getDimensionPixelSize(R.dimen.radius_sm));
        return button;
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

            if (getItemQuantity(itemId) > 0) {
                selectedCount++;
            }
        }

        itemBinding.tvSelectedCount.setText(getString(R.string.victim_supply_selected_count_format, selectedCount));
    }

    private int getItemQuantity(String itemId) {
        Integer quantity = itemQuantities.get(itemId);
        return quantity != null ? quantity : 0;
    }

    private void updateItemQuantity(String itemId, int delta, TextView countView) {
        int current = getItemQuantity(itemId);
        int next = Math.max(0, current + delta);
        itemQuantities.put(itemId, next);
        countView.setText(String.valueOf(next));
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
        for (Map.Entry<String, Integer> entry : itemQuantities.entrySet()) {
            String itemId = entry.getKey();
            Integer quantity = entry.getValue();

            if (itemId == null || itemId.trim().isEmpty() || quantity == null || quantity <= 0) {
                continue;
            }

            requestedItems.add(new VictimSupplyViewModel.RequestedItem(itemId.trim(), quantity));
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

        viewModel.submitRequest(adultCount, elderlyCount, childCount, notes, requestedItems);
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

        for (String itemId : new ArrayList<>(itemQuantities.keySet())) {
            itemQuantities.put(itemId, 0);
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
}
