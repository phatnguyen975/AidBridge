package com.drc.aidbridge.data.remote.dto.request.victim;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class UpdateRequestLocationRequest {

    @SerializedName("lat")
    private final double lat;

    @SerializedName("lng")
    private final double lng;

    @Nullable
    @SerializedName("accuracy")
    private final Double accuracy;

    @Nullable
    @SerializedName("capturedAt")
    private final String capturedAt;

    public UpdateRequestLocationRequest(double lat,
                                        double lng,
                                        @Nullable Double accuracy,
                                        @Nullable String capturedAt) {
        this.lat = lat;
        this.lng = lng;
        this.accuracy = accuracy;
        this.capturedAt = capturedAt;
    }
}
