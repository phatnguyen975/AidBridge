package com.drc.aidbridge.data.remote.dto.request.sponsor;

import com.google.gson.annotations.SerializedName;

public class CreateDonationItemRequest {

    @SerializedName("itemName")
    private final String itemName;

    @SerializedName("itemCategoryId")
    private final String itemCategoryId;

    @SerializedName(value = "quantity")
    private final int quantity;

    @SerializedName(value = "unit")
    private final String unit;

    @SerializedName(value = "description")
    private final String description;

    @SerializedName("expiryDate")
    private final String expiryDate;

    @SerializedName("imageUrl")
    private final String imageUrl;

    public CreateDonationItemRequest(String itemName,
                                     String itemCategoryId,
                                     int quantity,
                                     String unit,
                                     String description,
                                     String expiryDate,
                                     String imageUrl) {
        this.itemName = itemName;
        this.itemCategoryId = itemCategoryId;
        this.quantity = quantity;
        this.unit = unit;
        this.description = description;
        this.expiryDate = expiryDate;
        this.imageUrl = imageUrl;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemCategoryId() {
        return itemCategoryId;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public String getDescription() {
        return description;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
