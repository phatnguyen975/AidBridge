package com.drc.aidbridge.ui.main.adapter.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemAdminInventoryCategoryBinding;
import com.drc.aidbridge.databinding.ItemAdminInventoryChildBinding;
import com.drc.aidbridge.domain.model.admin.Hub;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AdminHubDetailInventoryAdapter
        extends RecyclerView.Adapter<AdminHubDetailInventoryAdapter.InventoryCategoryViewHolder> {

    private final List<Hub.InventoryItem> categories = new ArrayList<>();
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
        Hub.InventoryItem category = categories.get(position);
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

    public void submitList(@NonNull List<Hub.InventoryItem> data) {
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

        void bind(@NonNull Hub.InventoryItem category, boolean expanded) {
            String categoryName = safeText(category.getItemCategoryName());
            int currentQuantity = safeNonNegative(category.getCurrentQuantity());
            int lowStockThreshold = safeNonNegative(category.getLowStockThreshold());

            binding.imageCategoryIcon.setImageResource(resolveCategoryIcon(categoryName));
            binding.textCategoryName.setText(categoryName);
            binding.textCategoryThreshold.setText(binding.getRoot().getContext().getString(
                    R.string.admin_hub_detail_threshold_format,
                    String.valueOf(lowStockThreshold)));

            if (currentQuantity >= lowStockThreshold) {
                binding.textCategoryStatus.setText(R.string.admin_hub_detail_badge_ok);
                binding.textCategoryStatus.setBackgroundResource(R.drawable.bg_admin_inventory_badge_ok);
                binding.textCategoryStatus.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.admin_inventory_badge_ok_text));
            } else {
                binding.textCategoryStatus.setText(R.string.admin_hub_detail_badge_low);
                binding.textCategoryStatus.setBackgroundResource(R.drawable.bg_admin_inventory_badge_low);
                binding.textCategoryStatus.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.admin_inventory_badge_low_text));
            }

            binding.layoutInventoryChildren.removeAllViews();
            binding.layoutInventoryChildren.setVisibility(expanded ? View.VISIBLE : View.GONE);
            if (expanded) {
                LayoutInflater inflater = LayoutInflater.from(binding.getRoot().getContext());
                ItemAdminInventoryChildBinding childBinding = ItemAdminInventoryChildBinding.inflate(
                        inflater,
                        binding.layoutInventoryChildren,
                        false);
                childBinding.textInventoryItemName.setText(categoryName);
                childBinding.textInventoryItemQuantity.setText(
                        binding.getRoot().getContext().getString(
                                R.string.admin_hub_detail_inventory_quantity_format,
                                currentQuantity));
                binding.layoutInventoryChildren.addView(childBinding.getRoot());
            }
        }

        @NonNull
        private String safeText(String value) {
            if (value == null || value.trim().isEmpty()) {
                return binding.getRoot().getContext().getString(R.string.admin_hub_detail_inventory_name_fallback);
            }
            return value.trim();
        }

        private int safeNonNegative(Integer value) {
            return value == null ? 0 : Math.max(value, 0);
        }

        private int resolveCategoryIcon(@NonNull String categoryName) {
            String normalized = normalizeSearchText(categoryName);
            if (normalized.contains("thuoc") || normalized.contains("medicine")) {
                return R.drawable.ic_admin_category_medicine;
            }
            if (normalized.contains("quan ao") || normalized.contains("clothes")) {
                return R.drawable.ic_admin_category_clothes;
            }
            if (normalized.contains("thuc an") || normalized.contains("food")) {
                return R.drawable.ic_admin_category_food;
            }
            if (normalized.contains("nuoc") || normalized.contains("water")) {
                return R.drawable.ic_admin_category_water;
            }
            return R.drawable.ic_admin_category_other;
        }

        @NonNull
        private String normalizeSearchText(@NonNull String value) {
            String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .toLowerCase(Locale.getDefault());
            return normalized;
        }
    }
}
