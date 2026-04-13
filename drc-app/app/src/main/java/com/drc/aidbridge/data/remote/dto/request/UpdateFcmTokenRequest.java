package com.drc.aidbridge.data.remote.dto.request;

import com.google.gson.annotations.SerializedName;

public class UpdateFcmTokenRequest {

    @SerializedName("device_id")
    private final String deviceId;

    @SerializedName("fcm_token")
    private final String fcmToken;

    public UpdateFcmTokenRequest(String deviceId, String fcmToken) {
        this.deviceId = deviceId;
        this.fcmToken = fcmToken;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getFcmToken() {
        return fcmToken;
    }
}
