package com.drc.aidbridge.data.remote.dto.response.sponsor;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SponsorDonationHistoryItemResponse {

    @Nullable
    @SerializedName("id")
    private String id;

    @Nullable
    @SerializedName(value = "hub_id", alternate = {"hubId"})
    private String hubId;

    @Nullable
    @SerializedName(value = "qr_code_token", alternate = {"qrCodeToken"})
    private String qrCodeToken;

    @Nullable
    @SerializedName("status")
    private String status;

    @Nullable
    @SerializedName(value = "donation_code", alternate = {"donationCode"})
    private String donationCode;

    @Nullable
    @SerializedName(value = "created_at", alternate = {"createdAt"})
    private String createdAt;

    @Nullable
    @SerializedName("items")
    private List<SponsorDonationHistorySelectedItemResponse> items;

    @Nullable
    public String getId() {
        return id;
    }

    @Nullable
    public String getHubId() {
        return hubId;
    }

    @Nullable
    public String getQrCodeToken() {
        return qrCodeToken;
    }

    @Nullable
    public String getStatus() {
        return status;
    }

    @Nullable
    public String getDonationCode() {
        return donationCode;
    }

    @Nullable
    public String getCreatedAt() {
        return createdAt;
    }

    @Nullable
    public List<SponsorDonationHistorySelectedItemResponse> getItems() {
        return items;
    }
}
