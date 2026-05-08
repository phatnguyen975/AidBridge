package com.drc.aidbridge.ui.main.adapter.staff;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemStaffTaskBinding;
import com.drc.aidbridge.domain.model.staff.StaffUpcomingTask;

import java.util.ArrayList;
import java.util.List;

public class StaffTaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LOADING = 1;

    private final List<StaffUpcomingTask> items = new ArrayList<>();
    private boolean isLoadingMore = false;

    public StaffTaskAdapter() {
    }

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
            int verticalPadding = parent.getContext().getResources()
                .getDimensionPixelSize(R.dimen.staff_tasks_progress_margin_vertical);
            container.setPadding(0, verticalPadding, 0, verticalPadding);

            ProgressBar progressBar = new ProgressBar(parent.getContext());
            FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            progressParams.gravity = Gravity.CENTER;
            container.addView(progressBar, progressParams);
            return new LoadingViewHolder(container);
        }

        ItemStaffTaskBinding binding = ItemStaffTaskBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        );
        return new TaskViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof TaskViewHolder) {
            ((TaskViewHolder) holder).bind(items.get(position));
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

    public void addItems(@NonNull List<StaffUpcomingTask> newItems) {
        int start = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
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

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {

        private final ItemStaffTaskBinding binding;
        TaskViewHolder(@NonNull ItemStaffTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull StaffUpcomingTask item) {
            binding.tvTaskCode.setText(item.getCode());
            binding.tvPersonName.setText(item.getName());
            binding.tvPhone.setText(item.getPhone());
        }
    }
}
