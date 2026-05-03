package com.drc.aidbridge.domain.model.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminDashboardSummary {

    private final long totalHubs;
    private final long totalVolunteers;
    private final long todayMissions;
    private final long distributedItems;
    private final List<ItemCategoryStat> itemCategoryStats;
    private final long totalPackages;
    private final double packageGrowthPercent;
    private final long totalPeopleSupported;
    private final List<AdminAlert> alerts;
    private final List<RecentActivity> recentActivities;

    public AdminDashboardSummary(long totalHubs,
                                 long totalVolunteers,
                                 long todayMissions,
                                 long distributedItems,
                                 List<ItemCategoryStat> itemCategoryStats,
                                 long totalPackages,
                                 double packageGrowthPercent,
                                 long totalPeopleSupported,
                                 List<AdminAlert> alerts,
                                 List<RecentActivity> recentActivities) {
        this.totalHubs = totalHubs;
        this.totalVolunteers = totalVolunteers;
        this.todayMissions = todayMissions;
        this.distributedItems = distributedItems;
        this.itemCategoryStats = itemCategoryStats == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(itemCategoryStats));
        this.totalPackages = totalPackages;
        this.packageGrowthPercent = packageGrowthPercent;
        this.totalPeopleSupported = totalPeopleSupported;
        this.alerts = alerts == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(alerts));
        this.recentActivities = recentActivities == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(recentActivities));
    }

    public long getTotalHubs() {
        return totalHubs;
    }

    public long getTotalVolunteers() {
        return totalVolunteers;
    }

    public long getTodayMissions() {
        return todayMissions;
    }

    public long getDistributedItems() {
        return distributedItems;
    }

    public List<ItemCategoryStat> getItemCategoryStats() {
        return itemCategoryStats;
    }

    public long getTotalPackages() {
        return totalPackages;
    }

    public double getPackageGrowthPercent() {
        return packageGrowthPercent;
    }

    public long getTotalPeopleSupported() {
        return totalPeopleSupported;
    }

    public List<AdminAlert> getAlerts() {
        return alerts;
    }

    public List<RecentActivity> getRecentActivities() {
        return recentActivities;
    }

    public static class ItemCategoryStat {
        private final String category;
        private final long quantity;

        public ItemCategoryStat(String category, long quantity) {
            this.category = category == null ? "" : category.trim();
            this.quantity = Math.max(0L, quantity);
        }

        public String getCategory() {
            return category;
        }

        public long getQuantity() {
            return quantity;
        }
    }

    public static class AdminAlert {
        private final String id;
        private final String title;
        private final String message;
        private final String severity;

        public AdminAlert(String id, String title, String message, String severity) {
            this.id = id == null ? "" : id.trim();
            this.title = title == null ? "" : title.trim();
            this.message = message == null ? "" : message.trim();
            this.severity = severity == null ? "" : severity.trim().toUpperCase();
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getMessage() {
            return message;
        }

        public String getSeverity() {
            return severity;
        }
    }

    public static class RecentActivity {
        private final String id;
        private final String title;
        private final String description;
        private final String type;
        private final String createdAt;

        public RecentActivity(String id, String title, String description, String type, String createdAt) {
            this.id = id == null ? "" : id.trim();
            this.title = title == null ? "" : title.trim();
            this.description = description == null ? "" : description.trim();
            this.type = type == null ? "" : type.trim().toUpperCase();
            this.createdAt = createdAt == null ? "" : createdAt.trim();
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getType() {
            return type;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }
}
