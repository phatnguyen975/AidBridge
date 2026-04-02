package com.drc.aidbridge.ui.main.adapter.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemAdminInventoryCategoryBinding;
import com.drc.aidbridge.databinding.ItemAdminInventoryChildBinding;
import com.drc.aidbridge.ui.main.viewmodel.admin.AdminHubDetailViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdminHubDetailInventoryAdapter
        extends RecyclerView.Adapter<AdminHubDetailInventoryAdapter.InventoryCategoryViewHolder> {

    private final List<AdminHubDetailViewModel.InventoryCategory> categories = new ArrayList<>();
    private final Set<Integer> expandedPositions = new HashSet<>();

    @NonNull
    @Override
    public InventoryCategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemAdminInventoryCategoryBinding binding = ItemAdminInventoryCategoryBinding.inflate(inflater, parent, false);
        return new InventoryCategoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryCategoryViewHolder holder, int position) {
        AdminHubDetailViewModel.InventoryCategory category = categories.get(position);
        boolean isExpanded = expandedPositions.contains(position);
        holder.bind(category, isExpanded);

        holder.binding.layoutCategoryHeader.setOnClickListener(v -> {
            if (expandedPositions.contains(position)) {
                expandedPositions.remove(position);
            } else {
                expandedPositions.add(position);
            }
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void submitList(@NonNull List<AdminHubDetailViewModel.InventoryCategory> data) {
        categories.clear();
        categories.addAll(data);
        expandedPositions.clear();
        notifyDataSetChanged();
    }

    static class InventoryCategoryViewHolder extends RecyclerView.ViewHolder {

        final ItemAdminInventoryCategoryBinding binding;

        InventoryCategoryViewHolder(@NonNull ItemAdminInventoryCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull AdminHubDetailViewModel.InventoryCategory category, boolean expanded) {
            binding.imageCategoryIcon.setImageResource(category.iconResId);
            binding.textCategoryName.setText(category.nameResId);
            binding.textCategoryThreshold.setText(binding.getRoot().getContext().getString(
                    R.string.admin_hub_detail_threshold_format, String.valueOf(category.minimumUnits)));

            if (category.isEnough()) {
                binding.textCategoryStatus.setText(R.string.admin_hub_detail_badge_ok);
                binding.textCategoryStatus.setBackgroundResource(R.drawable.bg_admin_inventory_badge_ok);
                binding.textCategoryStatus.setTextColor(
                        binding.getRoot().getContext().getColor(R.color.admin_inventory_badge_ok_text));
            } else {
                binding.textCategoryStatus.setText(R.string.admin_hub_detail_badge_low);
                binding.textCategoryStatus.setBackgroundResource(R.drawable.bg_admin_inventory_badge_low);
                binding.textCategoryStatus.setTextColor(
                        binding.getRoot().getContext().getColor(R.color.admin_inventory_badge_low_text));
            }

            binding.layoutInventoryChildren.removeAllViews();
            binding.layoutInventoryChildren.setVisibility(expanded ? View.VISIBLE : View.GONE);
            if (expanded) {
                LayoutInflater inflater = LayoutInflater.from(binding.getRoot().getContext());
                for (AdminHubDetailViewModel.InventoryItem item : category.items) {
                    ItemAdminInventoryChildBinding childBinding = ItemAdminInventoryChildBinding.inflate(inflater,
                            binding.layoutInventoryChildren, false);
                    childBinding.textInventoryItemName.setText(item.nameResId);
                    String quantityText = binding.getRoot().getContext().getString(
                            R.string.admin_hub_item_quantity_format,
                            item.quantity,
                            binding.getRoot().getContext().getString(item.unitResId));
                    childBinding.textInventoryItemQuantity.setText(quantityText);
                    binding.layoutInventoryChildren.addView(childBinding.getRoot());
                }
            }
        }
    }
}
