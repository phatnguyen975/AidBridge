package com.drc.aidbridge.data.remote.dto.response.sponsor;

import com.google.gson.annotations.SerializedName;

public class SponsorDonationQrResponse {

    @SerializedName(value = "donation_id", alternate = { "donationId", "id" })
    private String donationId;

    @SerializedName(value = "donation_code", alternate = { "donationCode" })
    private String donationCode;

    @SerializedName(value = "qr_code_token", alternate = { "qrCodeToken" })
    private String qrCodeToken;

    public String getDonationId() {
        return donationId;
    }

    public String getQrCodeToken() {
        return qrCodeToken;
    }

    public String getDonationCode() {
        return donationCode;
    }
}
