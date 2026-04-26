package com.drc.aidbridge.ui.main.adapter.sponsor;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemSponsorHistoryCardBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for rendering sponsor donation history cards.
 */
public class SponsorHistoryAdapter extends RecyclerView.Adapter<SponsorHistoryAdapter.HistoryViewHolder> {

    private final List<HistoryItem> items = new ArrayList<>();
    private final OnHistoryItemClickListener onHistoryItemClickListener;

    public SponsorHistoryAdapter(@NonNull OnHistoryItemClickListener onHistoryItemClickListener) {
        this.onHistoryItemClickListener = onHistoryItemClickListener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemSponsorHistoryCardBinding binding = ItemSponsorHistoryCardBinding.inflate(inflater, parent, false);
        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem item = items.get(position);

        holder.binding.tvDate.setText(item.date);
        holder.binding.tvCategory.setText(item.category);
        holder.binding.tvQuantity.setText(holder.binding.getRoot().getContext().getString(
                R.string.sponsor_history_quantity_prefix
        ) + " " + item.quantity);
        holder.binding.tvHub.setText(item.hubName);
        holder.binding.tvStatus.setText(item.status);
        holder.binding.ivThumbnail.setImageResource(item.thumbnailResId);

        int statusColor = resolveStatusColor(holder, item.statusKey);
        int surfaceColor = ContextCompat.getColor(holder.binding.getRoot().getContext(), R.color.bg_surface_elevated);
        int softStatusColor = ColorUtils.blendARGB(statusColor, surfaceColor, 0.85f);

        holder.binding.tvStatus.setTextColor(statusColor);
        holder.binding.tvStatus.setBackgroundTintList(ColorStateList.valueOf(softStatusColor));

        Drawable statusBackground = holder.binding.tvStatus.getBackground();
        if (statusBackground instanceof GradientDrawable) {
            GradientDrawable chipDrawable = (GradientDrawable) statusBackground.mutate();
            chipDrawable.setStroke(
                    holder.binding.getRoot().getResources().getDimensionPixelSize(R.dimen.sponsor_recent_status_chip_stroke_width),
                    statusColor
            );
        }

        holder.binding.getRoot().setOnClickListener(v -> onHistoryItemClickListener.onHistoryItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitItems(@NonNull List<HistoryItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    public void addItems(@NonNull List<HistoryItem> newItems) {
        if (newItems.isEmpty()) {
            return;
        }
        int startPosition = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(startPosition, newItems.size());
    }

    private int resolveStatusColor(@NonNull HistoryViewHolder holder, @NonNull String statusKey) {
        if ("REGISTERED".equalsIgnoreCase(statusKey)) {
            return ContextCompat.getColor(holder.binding.getRoot().getContext(), R.color.warning_orange);
        }
        if ("RECEIVED".equalsIgnoreCase(statusKey)) {
            return ContextCompat.getColor(holder.binding.getRoot().getContext(), R.color.safe_green);
        }
        if ("OUTDATED".equalsIgnoreCase(statusKey)) {
            return ContextCompat.getColor(holder.binding.getRoot().getContext(), R.color.sos_red);
        }
        return ContextCompat.getColor(holder.binding.getRoot().getContext(), R.color.text_secondary);
    }

    public interface OnHistoryItemClickListener {
        void onHistoryItemClick(@NonNull HistoryItem item);
    }

    public static class HistoryItem {
        public final String id;
        public final String date;
        public final String category;
        public final String quantity;
        public final String hubName;
        public final String statusKey;
        public final String status;
        public final String itemSummary;
        public final String donationCode;
        public final String qrCodeToken;
        public final int thumbnailResId;

        public HistoryItem(@NonNull String id,
                           @NonNull String date,
                           @NonNull String category,
                           @NonNull String quantity,
                           @NonNull String hubName,
                           @NonNull String statusKey,
                           @NonNull String status,
                           @NonNull String itemSummary,
                           @NonNull String donationCode,
                           @NonNull String qrCodeToken,
                           int thumbnailResId) {
            this.id = id;
            this.date = date;
            this.category = category;
            this.quantity = quantity;
            this.hubName = hubName;
            this.statusKey = statusKey;
            this.status = status;
            this.itemSummary = itemSummary;
            this.donationCode = donationCode;
            this.qrCodeToken = qrCodeToken;
            this.thumbnailResId = thumbnailResId;
        }
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        final ItemSponsorHistoryCardBinding binding;

        HistoryViewHolder(@NonNull ItemSponsorHistoryCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
