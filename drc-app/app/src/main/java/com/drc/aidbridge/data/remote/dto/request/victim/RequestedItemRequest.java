package com.drc.aidbridge.data.remote.dto.request.victim;

import com.google.gson.annotations.SerializedName;

public class RequestedItemRequest {

    @SerializedName("itemCategoryId")
    private final String itemCategoryId;

    @SerializedName("quantity")
    private final int quantity;

    @SerializedName("description")
    private final String description;

    public RequestedItemRequest(String itemCategoryId, int quantity) {
        this(itemCategoryId, quantity, null);
    }

    public RequestedItemRequest(String itemCategoryId, int quantity, String description) {
        this.itemCategoryId = itemCategoryId;
        this.quantity = quantity;
        this.description = description;
    }

    public String getItemCategoryId() {
        return itemCategoryId;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getDescription() {
        return description;
    }
}
