package com.drc.aidbridge.domain.model.sponsor;

public class SponsorDonationItem {

    private final String itemName;
    private final String itemCategoryId;
    private final int quantity;
    private final String unit;
    private final String description;
    private final String expiryDate;
    private final String imageUrl;

    public SponsorDonationItem(String itemName,
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
