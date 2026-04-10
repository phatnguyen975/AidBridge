package com.drc.aidbridge.data.remote.dto.request;

import com.google.gson.annotations.SerializedName;

public class RequestOtpRequest {

    @SerializedName("email")
    private final String email;

    @SerializedName("phone_number")
    private final String phoneNumber;

    @SerializedName("otp_type")
    private final String otpType;

    public RequestOtpRequest(String email, String phoneNumber, String otpType) {
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.otpType = otpType;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getOtpType() {
        return otpType;
    }
}
