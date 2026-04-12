package com.drc.aidbridge.domain.model.victim;

public class VictimHistoryAidItemDetail {

    private final String categoryName;
    private final int quantity;
    private final String unit;

    public VictimHistoryAidItemDetail(String categoryName, int quantity, String unit) {
        this.categoryName = categoryName != null ? categoryName.trim() : "";
        this.quantity = Math.max(0, quantity);
        this.unit = unit != null ? unit.trim() : "";
    }

    public String getCategoryName() {
        return categoryName;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }
}
