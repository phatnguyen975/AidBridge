package com.drc.aidbridge.domain.model.staff;

import androidx.annotation.NonNull;

public class InventoryConfirmItem {

    private final String itemCategoryId;
    private final int quantity;

    public InventoryConfirmItem(String itemCategoryId, int quantity) {
        this.itemCategoryId = safeText(itemCategoryId);
        this.quantity = Math.max(quantity, 0);
    }

    @NonNull
    public String getItemCategoryId() {
        return itemCategoryId;
    }

    public int getQuantity() {
        return quantity;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
