package com.drc.aidbridge.domain.model.staff;

import androidx.annotation.NonNull;

public class InboundDraftItem {

    private final String parentCategoryId;
    private final String parentCategoryName;
    private final String itemCategoryId;
    private final String itemName;
    private final String unit;
    private final int quantity;
    private final String note;

    public InboundDraftItem(String parentCategoryId,
                            String parentCategoryName,
                            String itemCategoryId,
                            String itemName,
                            String unit,
                            int quantity,
                            String note) {
        this.parentCategoryId = safeText(parentCategoryId);
        this.parentCategoryName = safeText(parentCategoryName);
        this.itemCategoryId = safeText(itemCategoryId);
        this.itemName = safeText(itemName);
        this.unit = safeText(unit);
        this.quantity = Math.max(quantity, 0);
        this.note = safeText(note);
    }

    @NonNull
    public String getParentCategoryId() {
        return parentCategoryId;
    }

    @NonNull
    public String getParentCategoryName() {
        return parentCategoryName;
    }

    @NonNull
    public String getItemCategoryId() {
        return itemCategoryId;
    }

    @NonNull
    public String getItemName() {
        return itemName;
    }

    @NonNull
    public String getUnit() {
        return unit;
    }

    public int getQuantity() {
        return quantity;
    }

    @NonNull
    public String getNote() {
        return note;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
