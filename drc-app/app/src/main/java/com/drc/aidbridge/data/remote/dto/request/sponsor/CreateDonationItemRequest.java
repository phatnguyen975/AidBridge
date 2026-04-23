package com.drc.aidbridge.data.remote.dto.request.sponsor;

import com.google.gson.annotations.SerializedName;

public class CreateDonationItemRequest {

    @SerializedName("itemName")
    private final String itemName;

    @SerializedName(value = "quantity")
    private final int quantity;

    @SerializedName(value = "unit")
    private final String unit;

    @SerializedName(value = "description")
    private final String description;

    @SerializedName("imageUrl")
    private final String imageUrl;

    public CreateDonationItemRequest(String itemName,
                                     int quantity,
                                     String unit,
                                     String description,
                                     String imageUrl) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.unit = unit;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public String getItemName() {
        return itemName;
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

    public String getImageUrl() {
        return imageUrl;
    }
}
