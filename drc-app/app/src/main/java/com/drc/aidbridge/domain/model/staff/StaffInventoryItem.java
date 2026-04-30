package com.drc.aidbridge.domain.model.staff;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class StaffInventoryItem {

    private final String inventoryId;
    private final String itemCategoryId;
    private final String name;
    private final String unit;
    private final String iconUrl;
    private final String parentCategoryId;
    private final String parentCategoryName;
    private final int currentQuantity;
    private final int lowStockThreshold;
    private final boolean lowStock;
    private final String lastRestockedAt;

    public StaffInventoryItem(String inventoryId,
                              String itemCategoryId,
                              String name,
                              String unit,
                              String iconUrl,
                              String parentCategoryId,
                              String parentCategoryName,
                              int currentQuantity,
                              int lowStockThreshold,
                              boolean lowStock,
                              String lastRestockedAt) {
        this.inventoryId = safeText(inventoryId);
        this.itemCategoryId = safeText(itemCategoryId);
        this.name = safeText(name);
        this.unit = safeText(unit);
        this.iconUrl = safeNullableText(iconUrl);
        this.parentCategoryId = safeNullableText(parentCategoryId);
        this.parentCategoryName = safeText(parentCategoryName);
        this.currentQuantity = Math.max(currentQuantity, 0);
        this.lowStockThreshold = Math.max(lowStockThreshold, 0);
        this.lowStock = lowStock;
        this.lastRestockedAt = safeNullableText(lastRestockedAt);
    }

    @NonNull
    public String getInventoryId() {
        return inventoryId;
    }

    @NonNull
    public String getItemCategoryId() {
        return itemCategoryId;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getUnit() {
        return unit;
    }

    @Nullable
    public String getIconUrl() {
        return iconUrl;
    }

    @Nullable
    public String getParentCategoryId() {
        return parentCategoryId;
    }

    @NonNull
    public String getParentCategoryName() {
        return parentCategoryName;
    }

    public int getCurrentQuantity() {
        return currentQuantity;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public boolean isLowStock() {
        return lowStock;
    }

    @Nullable
    public String getLastRestockedAt() {
        return lastRestockedAt;
    }

    private String safeNullableText(String value) {
        String safe = safeText(value);
        return safe.isEmpty() ? null : safe;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
