package com.drc.aidbridge.data.remote.dto.request;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {

    @SerializedName("email")
    private final String email;

    @SerializedName("password")
    private final String password;

    @SerializedName("deviceId")
    private final String deviceId;

    @SerializedName("fcmToken")
    private final String fcmToken;

    public LoginRequest(String email, String password, String deviceId, String fcmToken) {
        this.email = email;
        this.password = password;
        this.deviceId = deviceId;
        this.fcmToken = fcmToken;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getFcmToken() {
        return fcmToken;
    }
}
