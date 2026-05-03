package com.drc.aidbridge.data.remote.dto.request.sponsor;

import com.google.gson.annotations.SerializedName;

public class CreateDonationItemRequest {

    @SerializedName("itemCategoryId")
    private final String itemCategoryId;

    public CreateDonationItemRequest(String itemCategoryId) {
        this.itemCategoryId = itemCategoryId;
    }

    public String getItemCategoryId() {
        return itemCategoryId;
    }
}
