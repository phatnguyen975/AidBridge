package com.drc.aidbridge.data.remote.dto.request;

import com.google.gson.annotations.SerializedName;

public class OtpVerifyRequest {

    @SerializedName("email")
    private final String email;

    @SerializedName("otp_code")
    private final String otpCode;

    @SerializedName("otp_type")
    private final String otpType;

    public OtpVerifyRequest(String email, String otpCode, String otpType) {
        this.email = email;
        this.otpCode = otpCode;
        this.otpType = otpType;
    }

    public String getEmail() {
        return email;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public String getOtpType() {
        return otpType;
    }
}
