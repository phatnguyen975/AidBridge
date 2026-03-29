package com.drc.aidbridge.ui.main.fragment.staff;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentStaffTaskDetailBottomSheetBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;

public class StaffTaskDetailBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "StaffTaskDetailBottomSheet";

    public static final String ARG_TASK_CODE = "arg_task_code";
    public static final String ARG_PERSON_NAME = "arg_person_name";
    public static final String ARG_PHONE = "arg_phone";
    public static final String ARG_STATUS = "arg_status";
    public static final String ARG_EXPECTED_ITEMS = "arg_expected_items";

    private FragmentStaffTaskDetailBottomSheetBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStaffTaskDetailBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindArguments();
        setupActions();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void bindArguments() {
        Bundle args = getArguments();
        String taskCode = args != null ? args.getString(ARG_TASK_CODE, "") : "";
        String personName = args != null ? args.getString(ARG_PERSON_NAME, "") : "";
        String phone = args != null ? args.getString(ARG_PHONE, "") : "";
        String status = args != null ? args.getString(ARG_STATUS, "") : "";
        ArrayList<String> expectedItems = args != null
            ? args.getStringArrayList(ARG_EXPECTED_ITEMS)
            : new ArrayList<>();

        binding.tvTitle.setText(taskCode);
        binding.tvPersonName.setText(personName);
        binding.tvPhone.setText(phone);
        binding.tvStatus.setText(status);
        applyStatusStyle(status);
        renderExpectedItems(expectedItems != null ? expectedItems : new ArrayList<>());
    }

    private void applyStatusStyle(@NonNull String status) {
        boolean isIncoming = getString(R.string.staff_task_status_incoming).contentEquals(status);
        if (isIncoming) {
            binding.tvStatus.setBackgroundResource(R.drawable.bg_staff_detail_status_chip);
            binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_primary));
        } else {
            binding.tvStatus.setBackgroundResource(R.drawable.bg_staff_task_status_completed);
            binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.safe_green));
        }
    }

    private void renderExpectedItems(@NonNull ArrayList<String> expectedItems) {
        binding.llExpectedItemsContainer.removeAllViews();

        int bottomMargin = getResources().getDimensionPixelSize(
            R.dimen.staff_task_sheet_expected_item_margin_bottom
        );
        float textSizePx = getResources().getDimension(R.dimen.staff_task_sheet_expected_item_size);

        for (String item : expectedItems) {
            TextView textView = new TextView(requireContext());
            textView.setText(getString(R.string.staff_task_detail_item_bullet_format, item));
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = bottomMargin;
            textView.setLayoutParams(params);

            binding.llExpectedItemsContainer.addView(textView);
        }
    }

    private void setupActions() {
        binding.btnClose.setOnClickListener(v -> dismiss());
    }
}
