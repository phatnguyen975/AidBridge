package com.drc.aidbridge.data.remote.dto.response.sponsor;

import com.google.gson.annotations.SerializedName;

public class SponsorDonationResponse {

    @SerializedName("id")
    private String id;

    @SerializedName(value = "qr_code_token", alternate = { "qrCodeToken" })
    private String qrCodeToken;

    public String getId() {
        return id;
    }

    public String getQrCodeToken() {
        return qrCodeToken;
    }
}
