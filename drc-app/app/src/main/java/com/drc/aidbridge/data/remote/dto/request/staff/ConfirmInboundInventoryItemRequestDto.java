package com.drc.aidbridge.data.remote.dto.request.staff;

import com.google.gson.annotations.SerializedName;

public class ConfirmInboundInventoryItemRequestDto {

    @SerializedName("parentCategoryId")
    private final String parentCategoryId;

    @SerializedName("itemCategoryId")
    private final String itemCategoryId;

    @SerializedName("quantity")
    private final int quantity;

    @SerializedName("note")
    private final String note;

    public ConfirmInboundInventoryItemRequestDto(String parentCategoryId,
                                                String itemCategoryId,
                                                int quantity,
                                                String note) {
        this.parentCategoryId = parentCategoryId;
        this.itemCategoryId = itemCategoryId;
        this.quantity = quantity;
        this.note = note;
    }
}
