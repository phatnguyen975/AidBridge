package com.drc.aidbridge.domain.model.sponsor;

public class SponsorDonationRequest {

    private final String hubId;
    private final String category;
    private final String itemName;
    private final int quantity;
    private final String unit;
    private final String description;
    private final String expectedTime;
    private final String imageUrl;

    public SponsorDonationRequest(String hubId,
                                  String category,
                                  String itemName,
                                  int quantity,
                                  String unit,
                                  String description,
                                  String expectedTime,
                                  String imageUrl) {
        this.hubId = hubId;
        this.category = category;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unit = unit;
        this.description = description;
        this.expectedTime = expectedTime;
        this.imageUrl = imageUrl;
    }

    public String getHubId() {
        return hubId;
    }

    public String getCategory() {
        return category;
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

    public String getExpectedTime() {
        return expectedTime;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
