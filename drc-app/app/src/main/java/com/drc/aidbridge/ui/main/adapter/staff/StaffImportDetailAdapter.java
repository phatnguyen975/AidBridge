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
import com.drc.aidbridge.databinding.ItemStaffImportDetailBinding;

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
        public final String name;
        public final int donationQuantity;
        public final String unit;

        public ImportDetailItem(String name, int donationQuantity, String unit) {
            this.name = name;
            this.donationQuantity = donationQuantity;
            this.unit = unit;
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
            ) + " " + item.donationQuantity + " " + item.unit);
            binding.ivStatus.setImageResource(R.drawable.ic_circle_check);
            binding.ivStatus.setColorFilter(ContextCompat.getColor(
                    binding.getRoot().getContext(),
                    R.color.safe_green
            ));
            binding.tvErrorBadge.setVisibility(android.view.View.GONE);
        }
    }
}
