package com.drc.aidbridge.domain.model.staff;

import androidx.annotation.NonNull;

public class InventoryQrPreviewItem {

    private final String itemCategoryId;
    private final String name;
    private final String unit;
    private final String parentCategoryName;
    private final int quantity;
    private final int requiredQuantity;
    private final int currentQuantity;
    private final boolean enoughStock;

    public InventoryQrPreviewItem(String itemCategoryId,
                                  String name,
                                  String unit,
                                  String parentCategoryName,
                                  int quantity,
                                  int requiredQuantity,
                                  int currentQuantity,
                                  boolean enoughStock) {
        this.itemCategoryId = safeText(itemCategoryId);
        this.name = safeText(name);
        this.unit = safeText(unit);
        this.parentCategoryName = safeText(parentCategoryName);
        this.quantity = Math.max(quantity, 0);
        this.requiredQuantity = Math.max(requiredQuantity, 0);
        this.currentQuantity = Math.max(currentQuantity, 0);
        this.enoughStock = enoughStock;
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

    @NonNull
    public String getParentCategoryName() {
        return parentCategoryName;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getRequiredQuantity() {
        return requiredQuantity;
    }

    public int getCurrentQuantity() {
        return currentQuantity;
    }

    public boolean isEnoughStock() {
        return enoughStock;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
