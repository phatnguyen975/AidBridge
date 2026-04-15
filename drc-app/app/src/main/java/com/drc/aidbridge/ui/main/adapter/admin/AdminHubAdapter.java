package com.drc.aidbridge.ui.main.adapter.admin;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemAdminHubBinding;
import com.drc.aidbridge.domain.enums.HubStatus;
import com.drc.aidbridge.domain.model.admin.Hub;

import java.util.ArrayList;
import java.util.List;

public class AdminHubAdapter extends RecyclerView.Adapter<AdminHubAdapter.AdminHubViewHolder> {

    private final List<Hub> items = new ArrayList<>();
    private final HubActionListener listener;

    public AdminHubAdapter(@NonNull HubActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AdminHubViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemAdminHubBinding binding = ItemAdminHubBinding.inflate(inflater, parent, false);
        return new AdminHubViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminHubViewHolder holder, int position) {
        Hub item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitList(@NonNull List<Hub> hubs) {
        items.clear();
        items.addAll(hubs);
        notifyDataSetChanged();
    }

    static class AdminHubViewHolder extends RecyclerView.ViewHolder {

        private final ItemAdminHubBinding binding;

        AdminHubViewHolder(@NonNull ItemAdminHubBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Hub hub, @NonNull HubActionListener listener) {
            binding.textHubName.setText(resolveText(hub.getName(), R.string.admin_hub_mgmt_name_fallback));
            binding.textHubAddress.setText(resolveText(hub.getAddress(), R.string.admin_hub_mgmt_address_fallback));
            binding.textHubOperatingHours.setText(resolveOperatingHoursText(hub.getOperatingHours()));

            HubStatus status = HubStatus.fromStringSafe(hub.getStatus());

            Glide.with(binding.imageHubThumbnail)
                    .load(hub.getImageUrl())
                    .placeholder(R.drawable.ic_hub_placeholder)
                    .error(R.drawable.ic_hub_placeholder)
                    .centerCrop()
                    .into(binding.imageHubThumbnail);

            if (status == HubStatus.ACTIVE) {
                applyActiveState();
            } else if (status == HubStatus.EMERGENCY) {
                applyInactiveState();
                binding.textHubStatus.setText(R.string.admin_hub_mgmt_status_emergency);
            } else {
                applyInactiveState();
            }

            binding.buttonHubDetails.setOnClickListener(v -> listener.onViewHubDetails(hub));
            binding.buttonHubToggleStatus.setOnClickListener(v -> listener.onToggleHubStatus(hub));
        }

        private void applyActiveState() {
            binding.textHubStatus.setText(R.string.admin_hub_mgmt_status_active);
            binding.textHubStatus.setBackgroundResource(R.drawable.bg_admin_badge_active);
            binding.textHubStatus.setTextColor(
                    ContextCompat.getColor(binding.getRoot().getContext(), R.color.admin_badge_active_text));
            binding.buttonHubToggleStatus.setText(R.string.admin_hub_mgmt_btn_deactivate);
            binding.buttonHubToggleStatus.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(
                            binding.getRoot().getContext(), R.color.admin_button_danger_bg)));
        }

        private void applyInactiveState() {
            binding.textHubStatus.setText(R.string.admin_hub_mgmt_status_inactive);
            binding.textHubStatus.setBackgroundResource(R.drawable.bg_admin_badge_suspended);
            binding.textHubStatus.setTextColor(
                    ContextCompat.getColor(binding.getRoot().getContext(), R.color.admin_badge_suspended_text));
            binding.buttonHubToggleStatus.setText(R.string.admin_hub_mgmt_btn_activate);
            binding.buttonHubToggleStatus.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(
                            binding.getRoot().getContext(), R.color.admin_button_success_bg)));
        }

        @NonNull
        private String resolveText(String value, @StringRes int fallbackRes) {
            if (value == null || value.trim().isEmpty()) {
                return binding.getRoot().getContext().getString(fallbackRes);
            }
            return value.trim();
        }

        @NonNull
        private String resolveOperatingHoursText(String operatingHours) {
            String content = operatingHours;
            if (content == null || content.trim().isEmpty()) {
                content = binding.getRoot().getContext().getString(R.string.admin_hub_mgmt_operating_hours_empty);
            } else {
                content = content.trim();
            }
            return binding.getRoot().getContext().getString(
                    R.string.admin_hub_mgmt_operating_hours_format,
                    content);
        }
    }

    public interface HubActionListener {
        void onViewHubDetails(@NonNull Hub hub);

        void onToggleHubStatus(@NonNull Hub hub);
    }
}