package com.drc.aidbridge.domain.model.sponsor;

public class SponsorDonationItem {

    private final String itemCategoryId;
    private final String displayName;

    public SponsorDonationItem(String itemCategoryId,
                               String displayName) {
        this.itemCategoryId = itemCategoryId;
        this.displayName = displayName;
    }

    public String getItemCategoryId() {
        return itemCategoryId;
    }

    public String getDisplayName() {
        return displayName;
    }
}
