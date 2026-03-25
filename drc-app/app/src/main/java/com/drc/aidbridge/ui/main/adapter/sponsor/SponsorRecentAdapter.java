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
import com.drc.aidbridge.databinding.ItemSponsorDashboardRecentBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for rendering recent sponsor donations on the dashboard.
 */
public class SponsorRecentAdapter extends RecyclerView.Adapter<SponsorRecentAdapter.SponsorRecentViewHolder> {

    private final List<SponsorRecentItem> items = new ArrayList<>();
    private final OnRecentClickListener onRecentClickListener;

    public SponsorRecentAdapter(@NonNull OnRecentClickListener onRecentClickListener) {
        this.onRecentClickListener = onRecentClickListener;
    }

    @NonNull
    @Override
    public SponsorRecentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemSponsorDashboardRecentBinding binding = ItemSponsorDashboardRecentBinding.inflate(inflater, parent, false);
        return new SponsorRecentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SponsorRecentViewHolder holder, int position) {
        SponsorRecentItem item = items.get(position);

        holder.binding.tvDate.setText(item.date);
        holder.binding.tvPrimary.setText(item.primaryText);
        holder.binding.tvSecondary.setText(item.secondaryText);
        holder.binding.tvStatus.setText(item.status);
        holder.binding.ivThumbnail.setImageResource(item.thumbnailResId);

        int surfaceColor = ContextCompat.getColor(holder.binding.getRoot().getContext(), R.color.bg_surface);
        int mutedColor = ColorUtils.blendARGB(item.statusBackgroundColor, surfaceColor, 0.72f);

        holder.binding.tvStatus.setBackgroundTintList(ColorStateList.valueOf(mutedColor));
        holder.binding.tvStatus.setTextColor(item.statusTextColor);

        Drawable statusBackground = holder.binding.tvStatus.getBackground();
        if (statusBackground instanceof GradientDrawable) {
            GradientDrawable chipDrawable = (GradientDrawable) statusBackground.mutate();
            int strokeWidthPx = holder.binding.getRoot().getResources()
                    .getDimensionPixelSize(R.dimen.sponsor_recent_status_chip_stroke_width);
            chipDrawable.setStroke(strokeWidthPx, item.statusBackgroundColor);
        }

        holder.binding.getRoot().setOnClickListener(v -> onRecentClickListener.onRecentClick(item));
        holder.binding.layoutDetail.setOnClickListener(v -> onRecentClickListener.onRecentClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitItems(@NonNull List<SponsorRecentItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public interface OnRecentClickListener {
        void onRecentClick(@NonNull SponsorRecentItem item);
    }

    public static class SponsorRecentItem {
        public final String date;
        public final String primaryText;
        public final String secondaryText;
        public final String status;
        public final int statusBackgroundColor;
        public final int statusTextColor;
        public final int thumbnailResId;

        public SponsorRecentItem(@NonNull String date,
                                 @NonNull String primaryText,
                                 @NonNull String secondaryText,
                                 @NonNull String status,
                                 int statusBackgroundColor,
                                 int statusTextColor,
                                 int thumbnailResId) {
            this.date = date;
            this.primaryText = primaryText;
            this.secondaryText = secondaryText;
            this.status = status;
            this.statusBackgroundColor = statusBackgroundColor;
            this.statusTextColor = statusTextColor;
            this.thumbnailResId = thumbnailResId;
        }
    }

    static class SponsorRecentViewHolder extends RecyclerView.ViewHolder {
        final ItemSponsorDashboardRecentBinding binding;

        SponsorRecentViewHolder(@NonNull ItemSponsorDashboardRecentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
