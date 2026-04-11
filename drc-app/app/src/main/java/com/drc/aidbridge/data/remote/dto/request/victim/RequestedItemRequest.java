package com.drc.aidbridge.data.remote.dto.request.victim;

import com.google.gson.annotations.SerializedName;

public class RequestedItemRequest {

    @SerializedName("itemId")
    private final String itemId;

    @SerializedName("quantity")
    private final int quantity;

    public RequestedItemRequest(String itemId, int quantity) {
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
