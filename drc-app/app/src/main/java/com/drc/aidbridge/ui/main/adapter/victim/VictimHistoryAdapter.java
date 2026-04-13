package com.drc.aidbridge.ui.main.adapter.victim;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemVictimHistoryCardBinding;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * VictimHistoryAdapter renders paginated history cards with dynamic icon/color per request type.
 */
public class VictimHistoryAdapter extends RecyclerView.Adapter<VictimHistoryAdapter.HistoryViewHolder> {

    private static final DateTimeFormatter HISTORY_DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US);

    public static final String TYPE_SOS_SELF = "TYPE_SOS_SELF";
    public static final String TYPE_SUPPLY = "TYPE_SUPPLY";
    public static final String TYPE_SOS_RELATIVE = "TYPE_SOS_RELATIVE";

    private final List<HistoryModel> items = new ArrayList<>();
    private final OnHistoryClickListener listener;

    public VictimHistoryAdapter(@NonNull OnHistoryClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemVictimHistoryCardBinding binding = ItemVictimHistoryCardBinding.inflate(inflater, parent, false);
        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryModel model = items.get(position);

        holder.binding.tvTitle.setText(resolveTitle(holder, model));
        holder.binding.tvDate.setText(resolveDate(holder, model));
        holder.binding.tvStatus.setText(resolveStatus(holder, model));

        int iconRes;
        int typeColorRes;

        switch (model.type) {
            case TYPE_SOS_SELF:
                iconRes = R.drawable.ic_rescue;
                typeColorRes = R.color.sos_red;
                break;
            case TYPE_SUPPLY:
                iconRes = R.drawable.ic_truck;
                typeColorRes = R.color.safe_green;
                break;
            case TYPE_SOS_RELATIVE:
            default:
                iconRes = R.drawable.ic_relative_support;
                typeColorRes = R.color.warning_orange;
                break;
        }

        int statusColorRes;
        switch (model.statusType) {
            case HistoryModel.STATUS_PENDING:
                statusColorRes = R.color.warning_orange;
                break;
            case HistoryModel.STATUS_PROCESSING:
                statusColorRes = R.color.hub_blue;
                break;
            case HistoryModel.STATUS_COMPLETED:
            default:
                statusColorRes = R.color.safe_green;
                break;
        }

        holder.binding.ivTypeIcon.setImageResource(iconRes);
        int baseTypeColor = ContextCompat.getColor(holder.binding.getRoot().getContext(), typeColorRes);
        int iconBgColor = ColorUtils.blendARGB(baseTypeColor, Color.WHITE, 0.65f);
        int iconBorderColor = ColorUtils.blendARGB(baseTypeColor, Color.WHITE, 0.30f);

        holder.binding.iconBox.setCardBackgroundColor(iconBgColor);
        holder.binding.iconBox.setStrokeColor(iconBorderColor);
        holder.binding.ivTypeIcon.setImageTintList(ColorStateList.valueOf(baseTypeColor));

        int statusColor = ContextCompat.getColor(holder.binding.getRoot().getContext(), statusColorRes);
        holder.binding.tvStatus.setTextColor(statusColor);
        Drawable statusDot = ContextCompat.getDrawable(holder.binding.getRoot().getContext(), R.drawable.ic_bullet);
        if (statusDot != null) {
            Drawable wrapped = DrawableCompat.wrap(statusDot.mutate());
            DrawableCompat.setTint(wrapped, statusColor);
            holder.binding.tvStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(wrapped, null, null, null);
        }

        View.OnClickListener clickListener = v -> listener.onHistoryClick(model);
        holder.binding.getRoot().setOnClickListener(clickListener);
        holder.binding.btnDetail.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItems(@NonNull List<HistoryModel> newItems) {
        int start = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    public interface OnHistoryClickListener {
        void onHistoryClick(@NonNull HistoryModel model);
    }

    public static class HistoryModel {
        public final String id;
        public final String title;
        public final String status;
        public final String statusType;
        public final String date;
        public final String location;
        public final String type;
        public final String detail;

        public static final String STATUS_PENDING = "STATUS_PENDING";
        public static final String STATUS_PROCESSING = "STATUS_PROCESSING";
        public static final String STATUS_COMPLETED = "STATUS_COMPLETED";

        public HistoryModel(String id,
                            String title,
                            String status,
                            String statusType,
                            String date,
                            String location,
                            String type,
                            String detail) {
            this.id = id;
            this.title = title;
            this.status = status;
            this.statusType = statusType;
            this.date = date;
            this.location = location;
            this.type = type;
            this.detail = detail;
        }
    }

    private String resolveTitle(@NonNull HistoryViewHolder holder, @NonNull HistoryModel model) {
        if (model.title != null && !model.title.trim().isEmpty()) {
            return model.title.trim();
        }

        switch (model.type) {
            case TYPE_SUPPLY:
                return holder.binding.getRoot().getContext().getString(R.string.victim_history_title_supply);
            case TYPE_SOS_RELATIVE:
                return holder.binding.getRoot().getContext().getString(R.string.victim_history_title_sos_relative);
            case TYPE_SOS_SELF:
            default:
                return holder.binding.getRoot().getContext().getString(R.string.victim_history_title_sos_self);
        }
    }

    private String resolveStatus(@NonNull HistoryViewHolder holder, @NonNull HistoryModel model) {
        if (model.status != null && !model.status.trim().isEmpty()) {
            return model.status.trim();
        }

        switch (model.statusType) {
            case HistoryModel.STATUS_PENDING:
                return holder.binding.getRoot().getContext().getString(R.string.victim_history_status_pending);
            case HistoryModel.STATUS_COMPLETED:
                return holder.binding.getRoot().getContext().getString(R.string.victim_history_status_completed);
            case HistoryModel.STATUS_PROCESSING:
            default:
                return holder.binding.getRoot().getContext().getString(R.string.victim_history_status_processing);
        }
    }

    private String resolveDate(@NonNull HistoryViewHolder holder, @NonNull HistoryModel model) {
        return model.date != null && !model.date.trim().isEmpty()
            ? formatDateTime(model.date.trim())
            : holder.binding.getRoot().getContext().getString(R.string.victim_history_placeholder_value);
    }

    private String formatDateTime(String rawDateTime) {
        try {
            Instant instant = Instant.parse(rawDateTime);
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return localDateTime.format(HISTORY_DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(rawDateTime);
            return localDateTime.format(HISTORY_DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
        }

        return rawDateTime;
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        final ItemVictimHistoryCardBinding binding;

        HistoryViewHolder(ItemVictimHistoryCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
