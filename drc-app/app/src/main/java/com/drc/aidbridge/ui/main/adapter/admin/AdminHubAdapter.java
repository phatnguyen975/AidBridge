package com.drc.aidbridge.ui.main.adapter.admin;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemAdminHubBinding;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminHubManagementViewModel;

import java.util.ArrayList;
import java.util.List;

public class AdminHubAdapter extends RecyclerView.Adapter<AdminHubAdapter.AdminHubViewHolder> {

    private final List<AdminHubManagementViewModel.Hub> items = new ArrayList<>();
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
        AdminHubManagementViewModel.Hub item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitList(@NonNull List<AdminHubManagementViewModel.Hub> hubs) {
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

        void bind(@NonNull AdminHubManagementViewModel.Hub hub, @NonNull HubActionListener listener) {
            binding.textHubName.setText(hub.nameResId);
            binding.textHubAddress.setText(hub.addressResId);

            String inventoryText = binding.getRoot().getContext()
                    .getString(R.string.admin_hub_mgmt_inventory_format,
                            binding.getRoot().getContext().getString(hub.inventoryResId));
            binding.textHubInventory.setText(inventoryText);

            if (hub.isActive) {
                binding.textHubStatus.setText(R.string.admin_hub_mgmt_status_active);
                binding.textHubStatus.setBackgroundResource(R.drawable.bg_admin_badge_active);
                binding.textHubStatus.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.admin_badge_active_text));
                binding.buttonHubToggleStatus.setText(R.string.admin_hub_mgmt_btn_deactivate);
                binding.buttonHubToggleStatus.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(
                                binding.getRoot().getContext(), R.color.admin_button_danger_bg)));
            } else {
                binding.textHubStatus.setText(R.string.admin_hub_mgmt_status_suspended);
                binding.textHubStatus.setBackgroundResource(R.drawable.bg_admin_badge_suspended);
                binding.textHubStatus.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.admin_badge_suspended_text));
                binding.buttonHubToggleStatus.setText(R.string.admin_hub_mgmt_btn_activate);
                binding.buttonHubToggleStatus.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(
                                binding.getRoot().getContext(), R.color.admin_button_success_bg)));
            }

            binding.buttonHubDetails.setOnClickListener(v -> {
                String hubName = binding.getRoot().getContext().getString(hub.nameResId);
                String message = binding.getRoot().getContext()
                        .getString(R.string.admin_hub_mgmt_toast_open_details, hubName);
                Toast.makeText(binding.getRoot().getContext(), message, Toast.LENGTH_SHORT).show();
            });
            binding.buttonHubToggleStatus.setOnClickListener(v -> listener.onToggleHubStatus(hub));
        }
    }

    public interface HubActionListener {
        void onToggleHubStatus(@NonNull AdminHubManagementViewModel.Hub hub);
    }
}