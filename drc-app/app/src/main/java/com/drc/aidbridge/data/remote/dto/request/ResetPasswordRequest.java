package com.drc.aidbridge.data.remote.dto.request;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordRequest {

    @SerializedName("email")
    private final String email;

    @SerializedName("otp_code")
    private final String otp;

    @SerializedName("new_password")
    private final String newPassword;

    public ResetPasswordRequest(String email, String otp, String newPassword) {
        this.email = email;
        this.otp = otp;
        this.newPassword = newPassword;
    }

    public String getEmail() {
        return email;
    }

    public String getOtp() {
        return otp;
    }

    public String getNewPassword() {
        return newPassword;
    }
}
