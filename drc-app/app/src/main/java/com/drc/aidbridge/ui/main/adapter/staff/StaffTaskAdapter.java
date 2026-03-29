package com.drc.aidbridge.ui.main.adapter.staff;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemStaffTaskBinding;
import com.drc.aidbridge.ui.main.fragment.staff.StaffTaskDetailBottomSheet;

import java.util.ArrayList;
import java.util.List;

public class StaffTaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LOADING = 1;

    public static final String TYPE_EXPORT = "export";
    public static final String TYPE_IMPORT = "import";

    private final List<TaskItem> items = new ArrayList<>();
    private final FragmentManager fragmentManager;
    private boolean isLoadingMore = false;

    public StaffTaskAdapter(@NonNull FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
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
        return new TaskViewHolder(binding, fragmentManager);
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

    public void addItems(@NonNull List<TaskItem> newItems) {
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
        private final FragmentManager fragmentManager;

        TaskViewHolder(@NonNull ItemStaffTaskBinding binding, @NonNull FragmentManager fragmentManager) {
            super(binding.getRoot());
            this.binding = binding;
            this.fragmentManager = fragmentManager;
        }

        void bind(@NonNull TaskItem item) {
            binding.tvEta.setText(item.eta);
            binding.tvStatus.setText(item.status);
            binding.tvTaskCode.setText(item.taskCode);
            binding.tvPersonName.setText(item.personName);

            boolean isIncoming = binding.getRoot().getContext().getString(R.string.staff_task_status_incoming)
                .contentEquals(item.status);
            if (isIncoming) {
                binding.tvStatus.setBackgroundResource(R.drawable.bg_staff_detail_status_chip);
                binding.tvStatus.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.color_primary));
            } else {
                binding.tvStatus.setBackgroundResource(R.drawable.bg_staff_task_status_completed);
                binding.tvStatus.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.safe_green));
            }

            binding.getRoot().setOnClickListener(v -> openDetailBottomSheet(item));
            binding.tvDetail.setOnClickListener(v -> openDetailBottomSheet(item));
        }

        private void openDetailBottomSheet(@NonNull TaskItem item) {
            Bundle args = new Bundle();
            args.putString(StaffTaskDetailBottomSheet.ARG_TASK_CODE, item.taskCode);
            args.putString(StaffTaskDetailBottomSheet.ARG_PERSON_NAME, item.personName);
            args.putString(StaffTaskDetailBottomSheet.ARG_PHONE, item.phone);
            args.putString(StaffTaskDetailBottomSheet.ARG_STATUS, item.status);
            args.putStringArrayList(
                StaffTaskDetailBottomSheet.ARG_EXPECTED_ITEMS,
                new ArrayList<>(item.mockItemsSummary)
            );

            StaffTaskDetailBottomSheet bottomSheet = new StaffTaskDetailBottomSheet();
            bottomSheet.setArguments(args);
            bottomSheet.show(fragmentManager, StaffTaskDetailBottomSheet.TAG);
        }
    }

    public static class TaskItem {

        public final String id;
        public final String type;
        public final String eta;
        public final String status;
        public final String taskCode;
        public final String personName;
        public final String phone;
        public final ArrayList<String> mockItemsSummary;

        public TaskItem(
            @NonNull String id,
            @NonNull String type,
            @NonNull String eta,
            @NonNull String status,
            @NonNull String taskCode,
            @NonNull String personName,
            @NonNull String phone,
            @NonNull ArrayList<String> mockItemsSummary
        ) {
            this.id = id;
            this.type = type;
            this.eta = eta;
            this.status = status;
            this.taskCode = taskCode;
            this.personName = personName;
            this.phone = phone;
            this.mockItemsSummary = mockItemsSummary;
        }
    }
}
