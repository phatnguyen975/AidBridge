package com.drc.aidbridge.domain.model.victim;

public class VictimRequestedItem {

    private final String itemId;
    private final int quantity;

    public VictimRequestedItem(String itemId, int quantity) {
        this.itemId = itemId != null ? itemId.trim() : "";
        this.quantity = Math.max(0, quantity);
    }

    public String getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }
}
