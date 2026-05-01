package com.drc.aidbridge.data.remote.dto.response.staff;

import com.google.gson.annotations.SerializedName;

public class CreateInboundSubCategoryResponseDto {

    @SerializedName("itemCategoryId")
    private String itemCategoryId;

    @SerializedName("parentCategoryId")
    private String parentCategoryId;

    @SerializedName("parentCategoryName")
    private String parentCategoryName;

    @SerializedName("name")
    private String name;

    @SerializedName("unit")
    private String unit;

    @SerializedName("isLeaf")
    private Boolean leaf;

    @SerializedName("message")
    private String message;

    public String getItemCategoryId() {
        return itemCategoryId;
    }

    public String getParentCategoryId() {
        return parentCategoryId;
    }

    public String getParentCategoryName() {
        return parentCategoryName;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public Boolean isLeaf() {
        return leaf;
    }

    public String getMessage() {
        return message;
    }
}
