package com.drc.aidbridge.ui.main.adapter.staff;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemStaffInventoryBinding;

import java.util.ArrayList;
import java.util.List;

public class StaffInventoryAdapter extends RecyclerView.Adapter<StaffInventoryAdapter.InventoryViewHolder> {

    private final List<InventoryItem> items = new ArrayList<>();

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemStaffInventoryBinding binding = ItemStaffInventoryBinding.inflate(inflater, parent, false);
        return new InventoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        InventoryItem item = items.get(position);
        holder.binding.tvItemName.setText(item.name);
        holder.binding.tvCategoryName.setText(item.categoryName);
        holder.binding.chipStatus.setText(item.status);
        holder.binding.tvQuantity.setText(String.valueOf(item.quantity));
        holder.binding.tvUnit.setText(item.unit);

        int borderColor = ContextCompat.getColor(holder.binding.getRoot().getContext(), item.leftBorderColorRes);
        holder.binding.viewLeftBorder.setBackgroundTintList(ColorStateList.valueOf(borderColor));

        int statusColorRes = resolveStatusColorRes(item.status, holder);
        int statusColor = ContextCompat.getColor(holder.binding.getRoot().getContext(), statusColorRes);
        int chipBackgroundColor = ColorUtils.blendARGB(
                statusColor,
                ContextCompat.getColor(holder.binding.getRoot().getContext(), R.color.bg_surface_elevated),
                0.86f
        );

        holder.binding.chipStatus.setTextColor(statusColor);
        holder.binding.chipStatus.setChipIconTint(ColorStateList.valueOf(statusColor));
        holder.binding.chipStatus.setChipStrokeColor(ColorStateList.valueOf(statusColor));
        holder.binding.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(chipBackgroundColor));

        if (holder.binding.chipStatus.getBackground() instanceof GradientDrawable) {
            GradientDrawable gradient = (GradientDrawable) holder.binding.chipStatus.getBackground().mutate();
            gradient.setStroke(
                    holder.binding.getRoot().getResources().getDimensionPixelSize(
                            R.dimen.staff_inventory_item_status_stroke_width
                    ),
                    statusColor
            );
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItems(@NonNull List<InventoryItem> newItems) {
        if (newItems.isEmpty()) {
            return;
        }
        int start = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    @ColorRes
    private int resolveStatusColorRes(@NonNull String status, @NonNull InventoryViewHolder holder) {
        String shortage = holder.binding.getRoot().getContext().getString(R.string.staff_inventory_status_shortage);
        String lowStock = holder.binding.getRoot().getContext().getString(R.string.staff_inventory_status_low_stock);
        String stable = holder.binding.getRoot().getContext().getString(R.string.staff_inventory_status_stable);

        if (shortage.equals(status)) {
            return R.color.sos_red;
        }
        if (lowStock.equals(status)) {
            return R.color.warning_orange;
        }
        if (stable.equals(status)) {
            return R.color.safe_green;
        }
        return R.color.text_secondary;
    }

    public static class InventoryItem {
        public final String id;
        public final String name;
        public final String categoryName;
        public final String status;
        public final int quantity;
        public final String unit;
        @ColorRes
        public final int leftBorderColorRes;

        public InventoryItem(@NonNull String id,
                             @NonNull String name,
                             @NonNull String categoryName,
                             @NonNull String status,
                             int quantity,
                             @NonNull String unit,
                             @ColorRes int leftBorderColorRes) {
            this.id = id;
            this.name = name;
            this.categoryName = categoryName;
            this.status = status;
            this.quantity = quantity;
            this.unit = unit;
            this.leftBorderColorRes = leftBorderColorRes;
        }
    }

    static class InventoryViewHolder extends RecyclerView.ViewHolder {
        final ItemStaffInventoryBinding binding;

        InventoryViewHolder(@NonNull ItemStaffInventoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
