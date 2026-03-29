package com.drc.aidbridge.ui.main.adapter.staff;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemStaffExportDetailBinding;

import java.util.ArrayList;
import java.util.List;

public class StaffExportDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LOADING = 1;

    private final List<ExportDetailItem> items = new ArrayList<>();
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

        ItemStaffExportDetailBinding binding = ItemStaffExportDetailBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        );
        return new ExportDetailViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ExportDetailViewHolder) {
            ((ExportDetailViewHolder) holder).bind(items.get(position));
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

    public void addItems(List<ExportDetailItem> newItems) {
        int start = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
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

    public static class ExportDetailItem {
        public final String name;
        public final int stockQuantity;
        public final int exportQuantity;
        public final String unit;

        public ExportDetailItem(String name, int stockQuantity, int exportQuantity, String unit) {
            this.name = name;
            this.stockQuantity = stockQuantity;
            this.exportQuantity = exportQuantity;
            this.unit = unit;
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class ExportDetailViewHolder extends RecyclerView.ViewHolder {

        private final ItemStaffExportDetailBinding binding;

        ExportDetailViewHolder(ItemStaffExportDetailBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ExportDetailItem item) {
            binding.tvName.setText(item.name);
            binding.tvStockValue.setText(binding.getRoot().getContext().getString(
                    R.string.staff_detail_stock_prefix
            ) + " " + item.stockQuantity + " " + item.unit);
            binding.tvExportValue.setText(binding.getRoot().getContext().getString(
                    R.string.staff_detail_export_prefix
            ) + " " + item.exportQuantity + " " + item.unit);

            boolean isInsufficient = item.stockQuantity < item.exportQuantity;
            if (isInsufficient) {
                binding.ivStatus.setImageResource(R.drawable.ic_info);
                binding.ivStatus.setColorFilter(ContextCompat.getColor(
                        binding.getRoot().getContext(),
                        R.color.sos_red
                ));
                binding.tvErrorBadge.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.ivStatus.setImageResource(R.drawable.ic_circle_check);
                binding.ivStatus.setColorFilter(ContextCompat.getColor(
                        binding.getRoot().getContext(),
                        R.color.safe_green
                ));
                binding.tvErrorBadge.setVisibility(android.view.View.GONE);
            }
        }
    }
}
