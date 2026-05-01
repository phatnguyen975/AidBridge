package com.drc.aidbridge.data.remote.dto.response.staff;

import com.google.gson.annotations.SerializedName;

public class InboundSubCategoryDto {

    @SerializedName("itemCategoryId")
    private String itemCategoryId;

    @SerializedName("name")
    private String name;

    @SerializedName("unit")
    private String unit;

    public String getItemCategoryId() {
        return itemCategoryId;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }
}
