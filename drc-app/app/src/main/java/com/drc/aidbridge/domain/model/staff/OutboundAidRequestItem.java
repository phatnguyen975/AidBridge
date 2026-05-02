package com.drc.aidbridge.domain.model.staff;

import androidx.annotation.NonNull;

public class OutboundAidRequestItem {

    private final String itemCategoryId;
    private final String name;
    private final String unit;
    private final int requestedQuantity;

    public OutboundAidRequestItem(String itemCategoryId,
                                  String name,
                                  String unit,
                                  int requestedQuantity) {
        this.itemCategoryId = safeText(itemCategoryId);
        this.name = safeText(name);
        this.unit = safeText(unit);
        this.requestedQuantity = Math.max(requestedQuantity, 0);
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

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
