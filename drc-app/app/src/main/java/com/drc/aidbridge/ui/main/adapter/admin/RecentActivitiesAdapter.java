package com.drc.aidbridge.ui.main.adapter.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemRecentActivityBinding;
import com.drc.aidbridge.domain.model.admin.AdminDashboardSummary;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class RecentActivitiesAdapter extends RecyclerView.Adapter<RecentActivitiesAdapter.RecentActivityViewHolder> {

    private final List<AdminDashboardSummary.RecentActivity> items = new ArrayList<>();
    private final SimpleDateFormat fallbackDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));
    private final List<SimpleDateFormat> parserFormats = createParsers();

    @NonNull
    @Override
    public RecentActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemRecentActivityBinding binding = ItemRecentActivityBinding.inflate(inflater, parent, false);
        return new RecentActivityViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentActivityViewHolder holder, int position) {
        holder.bind(items.get(position), getRelativeTime(items.get(position).getCreatedAt()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitList(List<AdminDashboardSummary.RecentActivity> activities) {
        items.clear();
        if (activities != null) {
            items.addAll(activities);
        }
        notifyDataSetChanged();
    }

    class RecentActivityViewHolder extends RecyclerView.ViewHolder {

        private final ItemRecentActivityBinding binding;

        RecentActivityViewHolder(@NonNull ItemRecentActivityBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull AdminDashboardSummary.RecentActivity activity, @NonNull String relativeTime) {
            binding.textActivityTitle.setText(
                    activity.getTitle().isEmpty()
                            ? binding.getRoot().getContext().getString(R.string.admin_ai_activity_default_title)
                            : activity.getTitle()
            );
            binding.textActivityDescription.setText(
                    activity.getDescription().isEmpty()
                            ? binding.getRoot().getContext().getString(R.string.admin_ai_activity_default_description)
                            : activity.getDescription()
            );
            binding.textActivityTime.setText(relativeTime);
            binding.imageActivityIcon.setImageResource(resolveIcon(activity.getType()));
        }
    }

    private int resolveIcon(String type) {
        if ("SOS".equalsIgnoreCase(type)) {
            return R.drawable.ic_admin_warning_overload;
        }
        if ("MISSION".equalsIgnoreCase(type)) {
            return R.drawable.ic_admin_mission;
        }
        if ("HUB".equalsIgnoreCase(type)) {
            return R.drawable.ic_admin_hub;
        }
        if ("DELIVERY".equalsIgnoreCase(type)) {
            return R.drawable.ic_admin_category_water;
        }
        if ("USER".equalsIgnoreCase(type)) {
            return R.drawable.ic_admin_user;
        }
        return R.drawable.ic_admin_settings;
    }

    private String getRelativeTime(String createdAt) {
        if (createdAt == null || createdAt.trim().isEmpty()) {
            return "Vừa xong";
        }

        Date eventTime = parseDate(createdAt);
        if (eventTime == null) {
            return "Không rõ thời gian";
        }

        long diffMillis = Math.max(0L, System.currentTimeMillis() - eventTime.getTime());
        long minute = 60_000L;
        long hour = 60L * minute;
        long day = 24L * hour;

        if (diffMillis < minute) {
            return "Vừa xong";
        }
        if (diffMillis < hour) {
            return (diffMillis / minute) + " phút trước";
        }
        if (diffMillis < day) {
            return (diffMillis / hour) + " giờ trước";
        }
        if (diffMillis < 7L * day) {
            return (diffMillis / day) + " ngày trước";
        }
        return fallbackDateFormat.format(eventTime);
    }

    private Date parseDate(String createdAt) {
        for (SimpleDateFormat parser : parserFormats) {
            try {
                return parser.parse(createdAt.trim());
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private List<SimpleDateFormat> createParsers() {
        List<SimpleDateFormat> parsers = new ArrayList<>();
        SimpleDateFormat formatMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US);
        formatMillis.setTimeZone(TimeZone.getTimeZone("UTC"));
        parsers.add(formatMillis);

        SimpleDateFormat formatSeconds = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US);
        formatSeconds.setTimeZone(TimeZone.getTimeZone("UTC"));
        parsers.add(formatSeconds);

        SimpleDateFormat formatNoZone = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        formatNoZone.setTimeZone(TimeZone.getDefault());
        parsers.add(formatNoZone);
        return parsers;
    }
}
