package com.drc.aidbridge.ui.main.adapter.staff;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemStaffInventoryBinding;
import com.drc.aidbridge.domain.model.staff.StaffInventoryItem;

import java.util.ArrayList;
import java.util.List;

public class StaffInventoryAdapter extends RecyclerView.Adapter<StaffInventoryAdapter.InventoryViewHolder> {

    private final List<StaffInventoryItem> items = new ArrayList<>();

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemStaffInventoryBinding binding = ItemStaffInventoryBinding.inflate(inflater, parent, false);
        return new InventoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        StaffInventoryItem item = items.get(position);
        Context context = holder.binding.getRoot().getContext();

        holder.binding.tvItemName.setText(item.getName());
        holder.binding.tvCategoryName.setText(item.getParentCategoryName());
        holder.binding.tvQuantity.setText(String.valueOf(item.getCurrentQuantity()));
        holder.binding.tvUnit.setText(item.getUnit());
        holder.binding.tvThreshold.setText(context.getString(
                R.string.staff_inventory_low_threshold_format,
                item.getLowStockThreshold(),
                item.getUnit()
        ));

        Glide.with(holder.binding.ivItemThumbnail)
                .load(item.getIconUrl())
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(holder.binding.ivItemThumbnail);

        bindStockStatus(holder, item.isLowStock());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitList(@NonNull List<StaffInventoryItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    private void bindStockStatus(@NonNull InventoryViewHolder holder, boolean isLowStock) {
        Context context = holder.binding.getRoot().getContext();
        int statusColorRes = isLowStock ? R.color.warning_orange : R.color.safe_green;
        int statusColor = ContextCompat.getColor(context, statusColorRes);
        int chipBackgroundColor = ColorUtils.blendARGB(
                statusColor,
                ContextCompat.getColor(context, R.color.bg_surface_elevated),
                0.86f
        );

        holder.binding.viewLeftBorder.setBackgroundTintList(ColorStateList.valueOf(statusColor));
        holder.binding.chipStatus.setText(isLowStock
                ? R.string.staff_inventory_status_low_stock
                : R.string.staff_inventory_status_stable);
        holder.binding.chipStatus.setTextColor(statusColor);
        holder.binding.chipStatus.setChipStrokeColor(ColorStateList.valueOf(statusColor));
        holder.binding.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(chipBackgroundColor));
    }

    static class InventoryViewHolder extends RecyclerView.ViewHolder {
        final ItemStaffInventoryBinding binding;

        InventoryViewHolder(@NonNull ItemStaffInventoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
