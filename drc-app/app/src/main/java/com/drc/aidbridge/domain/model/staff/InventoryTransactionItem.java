package com.drc.aidbridge.domain.model.staff;

import androidx.annotation.NonNull;

public class InventoryTransactionItem {

    private final String itemCategoryId;
    private final String name;
    private final int quantityDelta;
    private final int quantityAfter;

    public InventoryTransactionItem(String itemCategoryId,
                                    String name,
                                    int quantityDelta,
                                    int quantityAfter) {
        this.itemCategoryId = safeText(itemCategoryId);
        this.name = safeText(name);
        this.quantityDelta = quantityDelta;
        this.quantityAfter = Math.max(quantityAfter, 0);
    }

    @NonNull
    public String getItemCategoryId() {
        return itemCategoryId;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public int getQuantityDelta() {
        return quantityDelta;
    }

    public int getQuantityAfter() {
        return quantityAfter;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
