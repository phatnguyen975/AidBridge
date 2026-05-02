package com.drc.aidbridge.data.remote.dto.response.staff;

import com.google.gson.annotations.SerializedName;

public class InventoryQrPreviewItemDto {

    @SerializedName("itemCategoryId")
    private String itemCategoryId;

    @SerializedName("name")
    private String name;

    @SerializedName("unit")
    private String unit;

    @SerializedName("parentCategoryName")
    private String parentCategoryName;

    @SerializedName("quantity")
    private Integer quantity;

    @SerializedName("requiredQuantity")
    private Integer requiredQuantity;

    @SerializedName("currentQuantity")
    private Integer currentQuantity;

    @SerializedName("isEnoughStock")
    private Boolean enoughStock;

    public String getItemCategoryId() {
        return itemCategoryId;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public String getParentCategoryName() {
        return parentCategoryName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getRequiredQuantity() {
        return requiredQuantity;
    }

    public Integer getCurrentQuantity() {
        return currentQuantity;
    }

    public Boolean isEnoughStock() {
        return enoughStock;
    }
}
