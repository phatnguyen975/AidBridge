package com.drc.aidbridge.data.remote.dto.response.staff;

import com.google.gson.annotations.SerializedName;

public class InventoryTransactionItemDto {

    @SerializedName("itemCategoryId")
    private String itemCategoryId;

    @SerializedName("parentCategoryId")
    private String parentCategoryId;

    @SerializedName("parentCategoryName")
    private String parentCategoryName;

    @SerializedName("name")
    private String name;

    @SerializedName("itemName")
    private String itemName;

    @SerializedName("quantityDelta")
    private Integer quantityDelta;

    @SerializedName("quantityAfter")
    private Integer quantityAfter;

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
        return name != null ? name : itemName;
    }

    public String getItemName() {
        return itemName;
    }

    public Integer getQuantityDelta() {
        return quantityDelta;
    }

    public Integer getQuantityAfter() {
        return quantityAfter;
    }
}
