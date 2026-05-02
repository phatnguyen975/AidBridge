package com.drc.aidbridge.data.remote.dto.request.staff;

import com.google.gson.annotations.SerializedName;

public class ConfirmInventoryItemRequestDto {

    @SerializedName("itemCategoryId")
    private final String itemCategoryId;

    @SerializedName("quantity")
    private final int quantity;

    public ConfirmInventoryItemRequestDto(String itemCategoryId, int quantity) {
        this.itemCategoryId = itemCategoryId;
        this.quantity = quantity;
    }
}
