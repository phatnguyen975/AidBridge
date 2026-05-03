package com.drc.aidbridge.ui.main.adapter.staff;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemStaffImportDetailBinding;
import com.drc.aidbridge.domain.model.staff.InventoryConfirmItem;
import com.drc.aidbridge.domain.model.staff.InventoryQrPreviewItem;
import com.drc.aidbridge.domain.model.staff.InboundDraftItem;

import java.util.ArrayList;
import java.util.List;

public class StaffImportDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LOADING = 1;

    private final List<ImportDetailItem> items = new ArrayList<>();
    private boolean isLoadingMore = false;

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            FrameLayout container = new FrameLayout(parent.getContext());
            RecyclerView.LayoutParams rootParams = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            container.setLayoutParams(rootParams);
            int verticalPadding = parent.getContext().getResources().getDimensionPixelSize(R.dimen.spacing_sm);
            container.setPadding(0, verticalPadding, 0, verticalPadding);

            ProgressBar progressBar = new ProgressBar(parent.getContext());
            FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            progressParams.gravity = android.view.Gravity.CENTER;
            container.addView(progressBar, progressParams);
            return new LoadingViewHolder(container);
        }

        ItemStaffImportDetailBinding binding = ItemStaffImportDetailBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        );
        return new ImportDetailViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ImportDetailViewHolder) {
            ((ImportDetailViewHolder) holder).bind(items.get(position));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoadingMore && position == items.size()) {
            return VIEW_TYPE_LOADING;
        }
        return VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return items.size() + (isLoadingMore ? 1 : 0);
    }

    public void addItems(List<ImportDetailItem> newItems) {
        int start = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    public void submitPreviewItems(List<InventoryQrPreviewItem> previewItems) {
        items.clear();
        if (previewItems != null) {
            for (InventoryQrPreviewItem previewItem : previewItems) {
                if (previewItem == null) {
                    continue;
                }
                items.add(new ImportDetailItem(
                        previewItem.getItemCategoryId(),
                        previewItem.getName(),
                        previewItem.getQuantity(),
                        previewItem.getUnit(),
                        previewItem.getParentCategoryName()
                ));
            }
        }
        isLoadingMore = false;
        notifyDataSetChanged();
    }

    public void submitDraftItems(List<InboundDraftItem> draftItems) {
        items.clear();
        if (draftItems != null) {
            for (InboundDraftItem draftItem : draftItems) {
                if (draftItem == null) {
                    continue;
                }
                items.add(new ImportDetailItem(
                        draftItem.getItemCategoryId(),
                        draftItem.getItemName(),
                        draftItem.getQuantity(),
                        draftItem.getUnit(),
                        draftItem.getParentCategoryName()
                ));
            }
        }
        isLoadingMore = false;
        notifyDataSetChanged();
    }

    public List<InventoryConfirmItem> getConfirmItems() {
        List<InventoryConfirmItem> confirmItems = new ArrayList<>();
        for (ImportDetailItem item : items) {
            confirmItems.add(new InventoryConfirmItem(item.itemCategoryId, item.confirmQuantity));
        }
        return confirmItems;
    }

    public int getDataCount() {
        return items.size();
    }

    public void setLoadingMore(boolean loadingMore) {
        if (isLoadingMore == loadingMore) {
            return;
        }
        isLoadingMore = loadingMore;
        if (loadingMore) {
            notifyItemInserted(items.size());
        } else {
            notifyItemRemoved(items.size());
        }
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    public static class ImportDetailItem {
        public final String itemCategoryId;
        public final String name;
        public final int donationQuantity;
        public final String unit;
        public final String parentCategoryName;
        public int confirmQuantity;

        public ImportDetailItem(String name, int donationQuantity, String unit) {
            this("", name, donationQuantity, unit, "");
        }

        public ImportDetailItem(String itemCategoryId,
                                String name,
                                int donationQuantity,
                                String unit,
                                String parentCategoryName) {
            this.itemCategoryId = itemCategoryId != null ? itemCategoryId : "";
            this.name = name;
            this.donationQuantity = donationQuantity;
            this.unit = unit;
            this.parentCategoryName = parentCategoryName != null ? parentCategoryName : "";
            this.confirmQuantity = Math.max(donationQuantity, 1);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class ImportDetailViewHolder extends RecyclerView.ViewHolder {

        private final ItemStaffImportDetailBinding binding;

        ImportDetailViewHolder(ItemStaffImportDetailBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ImportDetailItem item) {
            binding.tvName.setText(item.name);
            binding.tvDonationValue.setText(binding.getRoot().getContext().getString(
                    R.string.staff_detail_donation_prefix
            ) + " " + item.donationQuantity + " " + item.unit
                    + (item.parentCategoryName.isEmpty() ? "" : " - " + item.parentCategoryName));
            Object existingWatcher = binding.etQuantity.getTag();
            if (existingWatcher instanceof TextWatcher) {
                binding.etQuantity.removeTextChangedListener((TextWatcher) existingWatcher);
            }
            binding.etQuantity.setText(String.valueOf(item.confirmQuantity));
            TextWatcher watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    item.confirmQuantity = parsePositiveInt(s != null ? s.toString() : "");
                }
            };
            binding.etQuantity.addTextChangedListener(watcher);
            binding.etQuantity.setTag(watcher);
            binding.ivStatus.setImageResource(R.drawable.ic_circle_check);
            binding.ivStatus.setColorFilter(ContextCompat.getColor(
                    binding.getRoot().getContext(),
                    R.color.safe_green
            ));
            binding.tvErrorBadge.setVisibility(android.view.View.GONE);
        }

        private int parsePositiveInt(String value) {
            try {
                return Math.max(Integer.parseInt(value.trim()), 0);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
    }
}
