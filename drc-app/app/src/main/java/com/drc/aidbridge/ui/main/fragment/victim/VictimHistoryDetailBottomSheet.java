package com.drc.aidbridge.ui.main.fragment.victim;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.BottomSheetVictimHistoryDetailBinding;
import com.drc.aidbridge.ui.main.adapter.victim.VictimHistoryAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * VictimHistoryDetailBottomSheet renders request details in a large scrollable panel.
 */
public class VictimHistoryDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_ID = "arg_history_id";
    private static final String ARG_TITLE = "arg_history_title";
    private static final String ARG_STATUS = "arg_history_status";
    private static final String ARG_STATUS_TYPE = "arg_history_status_type";
    private static final String ARG_DATE = "arg_history_date";
    private static final String ARG_LOCATION = "arg_history_location";
    private static final String ARG_TYPE = "arg_history_type";
    private static final String ARG_DETAIL = "arg_history_detail";

    private BottomSheetVictimHistoryDetailBinding binding;

    public static VictimHistoryDetailBottomSheet newInstance(@NonNull VictimHistoryAdapter.HistoryModel model) {
        VictimHistoryDetailBottomSheet sheet = new VictimHistoryDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_ID, model.id);
        args.putString(ARG_TITLE, model.title);
        args.putString(ARG_STATUS, model.status);
        args.putString(ARG_STATUS_TYPE, model.statusType);
        args.putString(ARG_DATE, model.date);
        args.putString(ARG_LOCATION, model.location);
        args.putString(ARG_TYPE, model.type);
        args.putString(ARG_DETAIL, model.detail);
        sheet.setArguments(args);
        return sheet;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setDismissWithAnimation(true);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetVictimHistoryDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindArguments();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void bindArguments() {
        Bundle args = getArguments();
        String id = args != null ? args.getString(ARG_ID, "") : "";
        String title = args != null ? args.getString(ARG_TITLE, "") : "";
        String status = args != null ? args.getString(ARG_STATUS, "") : "";
        String statusType = args != null ? args.getString(ARG_STATUS_TYPE, "") : "";
        String date = args != null ? args.getString(ARG_DATE, "") : "";
        String location = args != null ? args.getString(ARG_LOCATION, "") : "";
        String type = args != null ? args.getString(ARG_TYPE, "") : "";
        String detail = args != null ? args.getString(ARG_DETAIL, "") : "";

        String fallbackText = getString(R.string.victim_history_detail_unknown);
        String titleText = !isBlank(title) ? title : mapTypeTitle(type);
        String statusText = !isBlank(status) ? status : mapStatusText(statusType);
        String typeText = mapTypeTitle(type);
        String safeId = !isBlank(id) ? id : fallbackText;
        String safeDate = !isBlank(date) ? date : fallbackText;
        String safeLocation = !isBlank(location) ? location : fallbackText;
        String safeDetail = !isBlank(detail)
            ? detail
            : getString(R.string.victim_history_detail_note_empty);

        binding.tvSheetTitle.setText(titleText);
        binding.tvDetailBody.setText(getString(
            R.string.victim_history_detail_content_template,
            safeId,
            typeText,
            statusText,
            safeDate,
            safeLocation,
            safeDetail
        ));
    }

    private String mapTypeTitle(String type) {
        if (VictimHistoryAdapter.TYPE_SUPPLY.equals(type)) {
            return getString(R.string.victim_history_title_supply);
        }
        if (VictimHistoryAdapter.TYPE_SOS_RELATIVE.equals(type)) {
            return getString(R.string.victim_history_title_sos_relative);
        }
        return getString(R.string.victim_history_title_sos_self);
    }

    private String mapStatusText(String statusType) {
        if (VictimHistoryAdapter.HistoryModel.STATUS_PENDING.equals(statusType)) {
            return getString(R.string.victim_history_status_pending);
        }
        if (VictimHistoryAdapter.HistoryModel.STATUS_COMPLETED.equals(statusType)) {
            return getString(R.string.victim_history_status_completed);
        }
        return getString(R.string.victim_history_status_processing);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
