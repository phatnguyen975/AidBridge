package com.drc.aidbridge.ui.main.fragment.victim;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVictimSupplyTabBinding;
import com.drc.aidbridge.databinding.ItemSupplyCategoryBinding;
import com.drc.aidbridge.ui.base.BaseFragment;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VictimSupplyTabFragment extends BaseFragment<FragmentVictimSupplyTabBinding> {

    private boolean isRecording;
    private AnimatorSet voicePulseAnimator;

    @Nullable
    @Override
    protected FragmentVictimSupplyTabBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentVictimSupplyTabBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        setupDynamicCategories();
        setupSteppers();
        setupModernVoiceInput();
        binding.btnSubmitSupply.setOnClickListener(v -> extractDataAndSubmit());
    }

    @Override
    protected void observeViewModel() {
        // TODO: Implement ViewModel observation for supply data when backend integration is ready.
    }

    private void setupDynamicCategories() {
        LinkedHashMap<String, List<String>> mockCategories = buildMockCategories();
        binding.containerCategories.removeAllViews();

        int index = 0;
        for (Map.Entry<String, List<String>> entry : mockCategories.entrySet()) {
            ItemSupplyCategoryBinding categoryBinding = ItemSupplyCategoryBinding.inflate(getLayoutInflater(), binding.containerCategories, false);
            categoryBinding.tvCategoryName.setText(entry.getKey());
            categoryBinding.tvSelectedCount.setText(getString(R.string.victim_supply_selected_count_format, 0));

            categoryBinding.layoutHeader.setOnClickListener(v -> toggleCategory(categoryBinding));

            for (String itemName : entry.getValue()) {
                MaterialCheckBox checkBox = new MaterialCheckBox(requireContext());
                checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
                checkBox.setText(itemName);
                checkBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> updateSelectedCount(categoryBinding));
                categoryBinding.layoutContent.addView(checkBox);
            }

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
    }

    private LinkedHashMap<String, List<String>> buildMockCategories() {
        LinkedHashMap<String, List<String>> mockCategories = new LinkedHashMap<>();

        List<String> foodItems = new ArrayList<>();
        foodItems.add(getString(R.string.victim_supply_item_rice));
        foodItems.add(getString(R.string.victim_supply_item_noodles));
        foodItems.add(getString(R.string.victim_supply_item_canned));

        List<String> waterItems = new ArrayList<>();
        waterItems.add(getString(R.string.victim_supply_item_water));
        waterItems.add(getString(R.string.victim_supply_item_milk));
        waterItems.add(getString(R.string.victim_supply_item_electrolyte));

        mockCategories.put(getString(R.string.victim_supply_category_food), foodItems);
        mockCategories.put(getString(R.string.victim_supply_category_water), waterItems);
        return mockCategories;
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

    private void updateSelectedCount(ItemSupplyCategoryBinding itemBinding) {
        int checkedCount = 0;
        int childCount = itemBinding.layoutContent.getChildCount();

        for (int i = 0; i < childCount; i++) {
            View child = itemBinding.layoutContent.getChildAt(i);
            if (child instanceof MaterialCheckBox && ((MaterialCheckBox) child).isChecked()) {
                checkedCount++;
            }
        }

        itemBinding.tvSelectedCount.setText(getString(R.string.victim_supply_selected_count_format, checkedCount));
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

    private void setupModernVoiceInput() {
        binding.btnVoiceToggle.setOnClickListener(v -> {
            isRecording = !isRecording;

            if (isRecording) {
                // TODO: Start actual voice recording and transcription logic
                binding.btnVoiceToggle.setIconResource(R.drawable.ic_pause);
                binding.tvVoiceStatus.setText(R.string.victim_supply_voice_recording);
                startVoicePulse();
            } else {
                // TODO: Stop voice recording and finalize transcription
                binding.btnVoiceToggle.setIconResource(R.drawable.ic_mic);
                binding.tvVoiceStatus.setText(R.string.victim_supply_voice_idle);
                stopVoicePulse();
            }
        });
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
        SupplyFormInput rawInput = collectRawInput();
        // TODO: Pass SupplyFormInput to ViewModel. Domain layer handles validation.
        Toast.makeText(requireContext(), "Đang gửi yêu cầu...", Toast.LENGTH_SHORT).show();
    }

    @Nullable
    private SupplyFormInput collectRawInput() {
        LinkedHashMap<String, List<String>> selectedCategories = new LinkedHashMap<>();

        int categoryCount = binding.containerCategories.getChildCount();
        for (int i = 0; i < categoryCount; i++) {
            View categoryView = binding.containerCategories.getChildAt(i);
            TextView categoryNameView = categoryView.findViewById(R.id.tv_category_name);
            LinearLayout contentLayout = categoryView.findViewById(R.id.layout_content);

            if (categoryNameView == null || contentLayout == null) {
                continue;
            }

            List<String> checkedItems = new ArrayList<>();
            int contentChildCount = contentLayout.getChildCount();

            for (int j = 0; j < contentChildCount; j++) {
                View optionView = contentLayout.getChildAt(j);
                if (optionView instanceof MaterialCheckBox) {
                    MaterialCheckBox checkBox = (MaterialCheckBox) optionView;
                    if (checkBox.isChecked() && checkBox.getText() != null) {
                        checkedItems.add(String.valueOf(checkBox.getText()));
                    }
                }
            }

            selectedCategories.put(categoryNameView.getText().toString(), checkedItems);
        }

        int adultCount = parseInt(String.valueOf(binding.tvCountAdult.getText()).trim());
        int elderlyCount = parseInt(String.valueOf(binding.tvCountElderly.getText()).trim());
        int childCount = parseInt(String.valueOf(binding.tvCountChild.getText()).trim());
        String notes = String.valueOf(binding.etNotes.getText()).trim();

        return new SupplyFormInput(selectedCategories, adultCount, elderlyCount, childCount, notes);
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

    private static final class SupplyFormInput {
        final LinkedHashMap<String, List<String>> selectedCategories;
        final int adultCount;
        final int elderlyCount;
        final int childCount;
        final String notes;

        SupplyFormInput(
                LinkedHashMap<String, List<String>> selectedCategories,
                int adultCount,
                int elderlyCount,
                int childCount,
                String notes
        ) {
            this.selectedCategories = selectedCategories;
            this.adultCount = adultCount;
            this.elderlyCount = elderlyCount;
            this.childCount = childCount;
            this.notes = notes;
        }
    }

    @Override
    public void onDestroyView() {
        stopVoicePulse();
        voicePulseAnimator = null;
        super.onDestroyView();
    }
}
