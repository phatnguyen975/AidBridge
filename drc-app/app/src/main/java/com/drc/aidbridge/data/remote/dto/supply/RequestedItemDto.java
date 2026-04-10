package com.drc.aidbridge.data.remote.dto.supply;

import com.google.gson.annotations.SerializedName;

public class RequestedItemDto {

    @SerializedName("itemId")
    private final String itemId;

    @SerializedName("quantity")
    private final int quantity;

    public RequestedItemDto(String itemId, int quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public String getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }
}
