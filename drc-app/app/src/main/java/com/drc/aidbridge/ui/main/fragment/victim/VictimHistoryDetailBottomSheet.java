package com.drc.aidbridge.ui.main.fragment.victim;

import android.app.Dialog;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BundleCompat;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.BottomSheetVictimHistoryDetailBinding;
import com.drc.aidbridge.ui.main.adapter.victim.VictimHistoryAdapter;
import com.drc.aidbridge.ui.main.viewmodel.victim.VictimHistoryViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * VictimHistoryDetailBottomSheet renders request details in a large scrollable panel.
 */
public class VictimHistoryDetailBottomSheet extends BottomSheetDialogFragment {

    private static final DateTimeFormatter DETAIL_DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US);
    private static final String ARG_DETAIL_MODEL = "arg_history_detail_model";

    private BottomSheetVictimHistoryDetailBinding binding;

    public static VictimHistoryDetailBottomSheet newInstance(
        @NonNull VictimHistoryViewModel.HistoryDetailUiModel model
    ) {
        VictimHistoryDetailBottomSheet sheet = new VictimHistoryDetailBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DETAIL_MODEL, model);
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
        VictimHistoryViewModel.HistoryDetailUiModel model = args != null
            ? BundleCompat.getSerializable(
                args,
                ARG_DETAIL_MODEL,
                VictimHistoryViewModel.HistoryDetailUiModel.class
            )
            : null;

        if (model == null) {
            binding.tvSheetTitle.setText(getString(R.string.victim_history_detail_title));
            binding.tvDetailBody.setText(getString(R.string.victim_history_detail_empty_error));
            return;
        }

        binding.tvSheetTitle.setText(mapTypeTitle(model.type));
        binding.tvDetailBody.setText(buildDetailBody(model));
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

    private CharSequence buildDetailBody(VictimHistoryViewModel.HistoryDetailUiModel model) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        appendLine(builder, R.string.victim_history_detail_label_request_type, mapTypeTitle(model.type));

        String statusText = !isBlank(model.status)
            ? model.status
            : mapStatusText(model.statusType);
        appendLine(builder, R.string.victim_history_detail_label_status, statusText);
        appendLine(builder, R.string.victim_history_detail_label_time, formatDateTime(model.date));
        appendLine(builder, R.string.victim_history_detail_label_location, model.location);

        if (VictimHistoryAdapter.TYPE_SUPPLY.equals(model.type)) {
            appendLine(builder, R.string.victim_history_detail_label_adult_count, toDisplayNumber(model.numberAdult));
            appendLine(builder, R.string.victim_history_detail_label_elderly_count, toDisplayNumber(model.numberElderly));
            appendLine(builder, R.string.victim_history_detail_label_children_count, toDisplayNumber(model.numberChildren));
            appendRequestedItems(builder, model.requestedItems);
        } else {
            appendLine(builder, R.string.victim_history_detail_label_condition, model.condition);
            appendLine(builder, R.string.victim_history_detail_label_people_count, toDisplayNumber(model.peopleCount));
            appendLine(builder, R.string.victim_history_detail_label_contact_name, model.noteFullName);
            appendLine(builder, R.string.victim_history_detail_label_contact_phone, model.notePhoneNumber);
            appendLine(builder, R.string.victim_history_detail_label_health_detail, model.noteHealthDetail);
        }

        return builder;
    }

    private void appendRequestedItems(
        SpannableStringBuilder builder,
        List<VictimHistoryViewModel.HistoryDetailAidItemUiModel> items
    ) {
        String label = getString(R.string.victim_history_detail_label_requested_items);
        int start = builder.length();
        builder.append(label).append(":");
        builder.setSpan(
            new StyleSpan(Typeface.BOLD),
            start,
            start + label.length() + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        builder.append('\n').append('\n');

        if (items == null || items.isEmpty()) {
            builder.append(getString(R.string.victim_history_detail_not_available));
            return;
        }

        List<String> itemNames = new java.util.ArrayList<>();
        String notAvailableText = getString(R.string.victim_history_detail_not_available);
        for (VictimHistoryViewModel.HistoryDetailAidItemUiModel item : items) {
            if (item == null) {
                continue;
            }

            String name = fallbackText(item.categoryName);
            if (!isBlank(name) && !notAvailableText.equals(name)) {
                itemNames.add(name);
            }
        }

        if (itemNames.isEmpty()) {
            builder.append(getString(R.string.victim_history_detail_not_available));
            return;
        }

        for (int index = 0; index < itemNames.size(); index++) {
            builder.append("\u2022 ")
                .append(itemNames.get(index));

            if (index < itemNames.size() - 1) {
                builder.append('\n').append('\n');
            }
        }
    }

    private void appendLine(SpannableStringBuilder builder, int labelRes, String value) {
        String label = getString(labelRes);
        int start = builder.length();

        builder.append(label).append(": ");
        builder.setSpan(
            new StyleSpan(Typeface.BOLD),
            start,
            start + label.length() + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        builder.append(fallbackText(value)).append('\n').append('\n');
    }

    private String toDisplayNumber(Integer value) {
        return value != null ? String.valueOf(Math.max(0, value)) : "";
    }

    private String fallbackText(String value) {
        return isBlank(value)
            ? getString(R.string.victim_history_detail_not_available)
            : value.trim();
    }

    private String formatDateTime(String rawDateTime) {
        if (isBlank(rawDateTime)) {
            return getString(R.string.victim_history_detail_not_available);
        }

        String trimmed = rawDateTime.trim();
        try {
            Instant instant = Instant.parse(trimmed);
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return localDateTime.format(DETAIL_DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(trimmed);
            return localDateTime.format(DETAIL_DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
        }

        return trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
