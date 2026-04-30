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

    public AdminDashboardSummary(long totalHubs,
                                 long totalVolunteers,
                                 long todayMissions,
                                 long distributedItems,
                                 List<ItemCategoryStat> itemCategoryStats) {
        this.totalHubs = totalHubs;
        this.totalVolunteers = totalVolunteers;
        this.todayMissions = todayMissions;
        this.distributedItems = distributedItems;
        this.itemCategoryStats = itemCategoryStats == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(itemCategoryStats));
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
}
