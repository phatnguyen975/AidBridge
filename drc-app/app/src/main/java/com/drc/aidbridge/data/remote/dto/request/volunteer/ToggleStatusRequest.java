package com.drc.aidbridge.data.remote.dto.request.volunteer;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class ToggleStatusRequest {

    @SerializedName("isOnline")
    private final boolean isOnline;

    @Nullable
    @SerializedName("currentLat")
    private final Double currentLat;

    @Nullable
    @SerializedName("currentLng")
    private final Double currentLng;

    public ToggleStatusRequest(boolean isOnline, @Nullable Double currentLat, @Nullable Double currentLng) {
        this.isOnline = isOnline;
        this.currentLat = currentLat;
        this.currentLng = currentLng;
    }

    public boolean isOnline() {
        return isOnline;
    }

    @Nullable
    public Double getCurrentLat() {
        return currentLat;
    }

    @Nullable
    public Double getCurrentLng() {
        return currentLng;
    }
}
