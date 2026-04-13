package com.drc.aidbridge.data.remote.dto.request;

import com.google.gson.annotations.SerializedName;

public class AcceptMissionRequest {

    @SerializedName("dispatchAttemptId")
    private final String dispatchAttemptId;

    @SerializedName("currentLat")
    private final Double currentLat;

    @SerializedName("currentLng")
    private final Double currentLng;

    public AcceptMissionRequest(String dispatchAttemptId, Double currentLat, Double currentLng) {
        this.dispatchAttemptId = dispatchAttemptId;
        this.currentLat = currentLat;
        this.currentLng = currentLng;
    }

    public String getDispatchAttemptId() {
        return dispatchAttemptId;
    }

    public Double getCurrentLat() {
        return currentLat;
    }

    public Double getCurrentLng() {
        return currentLng;
    }
}
