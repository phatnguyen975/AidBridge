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
}
