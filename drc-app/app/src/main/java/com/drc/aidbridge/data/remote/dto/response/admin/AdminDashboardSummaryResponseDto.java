package com.drc.aidbridge.data.remote.dto.response.admin;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AdminDashboardSummaryResponseDto {

    @Nullable
    @SerializedName("totalHubs")
    private Long totalHubs;

    @Nullable
    @SerializedName("totalVolunteers")
    private Long totalVolunteers;

    @Nullable
    @SerializedName("todayMissions")
    private Long todayMissions;

    @Nullable
    @SerializedName("distributedItems")
    private Long distributedItems;

    @Nullable
    @SerializedName("itemCategoryStats")
    private List<ItemCategoryStatDto> itemCategoryStats;

    @Nullable
    @SerializedName("totalPackages")
    private Long totalPackages;

    @Nullable
    @SerializedName("packageGrowthPercent")
    private Double packageGrowthPercent;

    @Nullable
    @SerializedName("totalPeopleSupported")
    private Long totalPeopleSupported;

    @Nullable
    @SerializedName("alerts")
    private List<AlertDto> alerts;

    @Nullable
    @SerializedName("recentActivities")
    private List<RecentActivityDto> recentActivities;

    @Nullable
    public Long getTotalHubs() {
        return totalHubs;
    }

    @Nullable
    public Long getTotalVolunteers() {
        return totalVolunteers;
    }

    @Nullable
    public Long getTodayMissions() {
        return todayMissions;
    }

    @Nullable
    public Long getDistributedItems() {
        return distributedItems;
    }

    @Nullable
    public List<ItemCategoryStatDto> getItemCategoryStats() {
        return itemCategoryStats;
    }

    @Nullable
    public Long getTotalPackages() {
        return totalPackages;
    }

    @Nullable
    public Double getPackageGrowthPercent() {
        return packageGrowthPercent;
    }

    @Nullable
    public Long getTotalPeopleSupported() {
        return totalPeopleSupported;
    }

    @Nullable
    public List<AlertDto> getAlerts() {
        return alerts;
    }

    @Nullable
    public List<RecentActivityDto> getRecentActivities() {
        return recentActivities;
    }

    public static class ItemCategoryStatDto {

        @Nullable
        @SerializedName("category")
        private String category;

        @Nullable
        @SerializedName("quantity")
        private Long quantity;

        @Nullable
        public String getCategory() {
            return category;
        }

        @Nullable
        public Long getQuantity() {
            return quantity;
        }
    }

    public static class AlertDto {

        @Nullable
        @SerializedName("id")
        private String id;

        @Nullable
        @SerializedName("title")
        private String title;

        @Nullable
        @SerializedName("message")
        private String message;

        @Nullable
        @SerializedName("severity")
        private String severity;

        @Nullable
        public String getId() {
            return id;
        }

        @Nullable
        public String getTitle() {
            return title;
        }

        @Nullable
        public String getMessage() {
            return message;
        }

        @Nullable
        public String getSeverity() {
            return severity;
        }
    }

    public static class RecentActivityDto {

        @Nullable
        @SerializedName("id")
        private String id;

        @Nullable
        @SerializedName("title")
        private String title;

        @Nullable
        @SerializedName("description")
        private String description;

        @Nullable
        @SerializedName("type")
        private String type;

        @Nullable
        @SerializedName("createdAt")
        private String createdAt;

        @Nullable
        public String getId() {
            return id;
        }

        @Nullable
        public String getTitle() {
            return title;
        }

        @Nullable
        public String getDescription() {
            return description;
        }

        @Nullable
        public String getType() {
            return type;
        }

        @Nullable
        public String getCreatedAt() {
            return createdAt;
        }
    }
}
