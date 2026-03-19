package com.drc.aidbridge.data.remote.dto.request;

import com.google.gson.annotations.SerializedName;

public class OtpVerifyRequest {

    @SerializedName("email")
    private final String email;

    @SerializedName("otpCode")
    private final String otpCode;

    public OtpVerifyRequest(String email, String otpCode) {
        this.email = email;
        this.otpCode = otpCode;
    }

    public String getEmail() {
        return email;
    }

    public String getOtpCode() {
        return otpCode;
    }
}
