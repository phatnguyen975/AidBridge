package com.drc.aidbridge.ui.main.adapter.volunteer;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.drc.aidbridge.R;
import com.drc.aidbridge.data.remote.dto.response.volunteer.MissionHistoryFullItemDto;
import com.drc.aidbridge.databinding.ItemVolunteerMissionHistoryBinding;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class VolunteerMissionHistoryFullAdapter
        extends RecyclerView.Adapter<VolunteerMissionHistoryFullAdapter.ViewHolder> {

    private final List<MissionHistoryFullItemDto> items = new ArrayList<>();
    private OnItemClickListener listener;

    @Inject
    public VolunteerMissionHistoryFullAdapter() {
    }

    public interface OnItemClickListener {
        void onItemClick(MissionHistoryFullItemDto item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemVolunteerMissionHistoryBinding binding = ItemVolunteerMissionHistoryBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MissionHistoryFullItemDto item = items.get(position);

        holder.binding.tvMissionCategory.setText(item.getMissionType());
        holder.binding.tvMissionLocation.setText(item.getAddress() != null ? item.getAddress() : "Không có địa chỉ");
        holder.binding.tvMissionDateTime.setText(formatToVnTime(item.getCreatedAt()));
        
        // Hiển thị Mã nhiệm vụ (Code Name)
        String codeName = item.getCodeName();
        if (codeName != null && !codeName.trim().isEmpty()) {
            holder.binding.tvMissionCodeName.setText("Mã: " + codeName.trim());
            holder.binding.tvMissionCodeName.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.binding.tvMissionCodeName.setVisibility(android.view.View.GONE);
        }

        // Set category badge color
        if ("RESCUE".equalsIgnoreCase(item.getMissionType())) {
            holder.binding.cardMissionCategoryBadge.setCardBackgroundColor(
                    holder.binding.getRoot().getContext().getColor(R.color.sos_red));
        } else {
            holder.binding.cardMissionCategoryBadge.setCardBackgroundColor(
                    holder.binding.getRoot().getContext().getColor(R.color.hub_blue));
        }

        // Radius
        if (item.getRadiusKm() != null) {
            holder.binding.tvMissionRadius.setText(String.format(java.util.Locale.getDefault(), "Bán kính: %.2f km", item.getRadiusKm()));
            holder.binding.tvMissionRadius.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.binding.tvMissionRadius.setVisibility(android.view.View.GONE);
        }

        // Status
        String status = item.getStatus() != null ? item.getStatus() : "";
        holder.binding.tvMissionStatus.setText(status);
        
        int bgColor = android.graphics.Color.parseColor("#E2E8F0");
        int textColor = android.graphics.Color.parseColor("#1E293B");

        if ("PENDING".equalsIgnoreCase(status)) {
            bgColor = android.graphics.Color.parseColor("#FEF3C7");
            textColor = android.graphics.Color.parseColor("#D97706");
        } else if ("ASSIGNED".equalsIgnoreCase(status)) {
            bgColor = android.graphics.Color.parseColor("#DBEAFE");
            textColor = android.graphics.Color.parseColor("#1D4ED8");
        } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            bgColor = android.graphics.Color.parseColor("#E0E7FF");
            textColor = android.graphics.Color.parseColor("#4338CA");
        } else if ("COMPLETED".equalsIgnoreCase(status)) {
            bgColor = android.graphics.Color.parseColor("#D1FAE5");
            textColor = android.graphics.Color.parseColor("#047857");
        } else if ("CANCELLED".equalsIgnoreCase(status)) {
            bgColor = android.graphics.Color.parseColor("#F1F5F9");
            textColor = android.graphics.Color.parseColor("#475569");
        }

        holder.binding.cardMissionStatusBadge.setCardBackgroundColor(bgColor);
        holder.binding.tvMissionStatus.setTextColor(textColor);

        // Photo
        String photoUrl = item.getImageUrl() != null ? item.getImageUrl() : item.getConfirmationImageUrl();
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            holder.binding.ivMissionPhoto.setVisibility(android.view.View.VISIBLE);
            Glide.with(holder.binding.ivMissionPhoto)
                    .load(photoUrl)
                    .centerCrop()
                    .into(holder.binding.ivMissionPhoto);
        } else {
            holder.binding.ivMissionPhoto.setVisibility(android.view.View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(@NonNull List<MissionHistoryFullItemDto> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemVolunteerMissionHistoryBinding binding;

        ViewHolder(ItemVolunteerMissionHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private String formatToVnTime(String isoString) {
        if (isoString == null || isoString.trim().isEmpty()) return "--";
        try {
            java.time.Instant instant = java.time.Instant.parse(isoString);
            java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
            return ldt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            return isoString;
        }
    }
}
