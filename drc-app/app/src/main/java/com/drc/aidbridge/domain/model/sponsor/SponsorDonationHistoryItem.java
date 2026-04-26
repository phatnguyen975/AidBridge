package com.drc.aidbridge.domain.model.sponsor;

public class SponsorDonationHistoryItem {

    private final String id;
    private final String donationCode;
    private final String qrCodeToken;
    private final SponsorDonationStatus status;
    private final String createdAt;
    private final String hubId;
    private final int itemCount;
    private final String itemSummary;

    public SponsorDonationHistoryItem(String id,
                                      String donationCode,
                                      String qrCodeToken,
                                      SponsorDonationStatus status,
                                      String createdAt,
                                      String hubId,
                                      int itemCount,
                                      String itemSummary) {
        this.id = id;
        this.donationCode = donationCode;
        this.qrCodeToken = qrCodeToken;
        this.status = status;
        this.createdAt = createdAt;
        this.hubId = hubId;
        this.itemCount = itemCount;
        this.itemSummary = itemSummary;
    }

    public String getId() {
        return id;
    }

    public String getDonationCode() {
        return donationCode;
    }

    public String getQrCodeToken() {
        return qrCodeToken;
    }

    public SponsorDonationStatus getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getHubId() {
        return hubId;
    }

    public int getItemCount() {
        return itemCount;
    }

    public String getItemSummary() {
        return itemSummary;
    }
}
