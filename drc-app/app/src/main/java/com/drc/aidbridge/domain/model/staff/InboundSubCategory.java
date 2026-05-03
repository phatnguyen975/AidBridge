package com.drc.aidbridge.domain.model.staff;

import androidx.annotation.NonNull;

public class InboundSubCategory {

    private final String itemCategoryId;
    private final String name;
    private final String unit;

    public InboundSubCategory(String itemCategoryId, String name, String unit) {
        this.itemCategoryId = safeText(itemCategoryId);
        this.name = safeText(name);
        this.unit = safeText(unit);
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

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
