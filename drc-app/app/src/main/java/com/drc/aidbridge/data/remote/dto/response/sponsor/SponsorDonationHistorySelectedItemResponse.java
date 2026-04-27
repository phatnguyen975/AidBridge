package com.drc.aidbridge.data.remote.dto.response.sponsor;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class SponsorDonationHistorySelectedItemResponse {

    @Nullable
    @SerializedName(value = "item_category_id", alternate = {"itemCategoryId"})
    private String itemCategoryId;

    @Nullable
    @SerializedName(value = "item_category_name", alternate = {"itemCategoryName", "name"})
    private String itemCategoryName;

    @Nullable
    public String getItemCategoryId() {
        return itemCategoryId;
    }

    @Nullable
    public String getItemCategoryName() {
        return itemCategoryName;
    }
}
