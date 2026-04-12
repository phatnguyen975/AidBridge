package com.drc.aidbridge.data.remote.dto.request.victim;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class CreateSosRequest {

    @SerializedName("lat")
    private final double lat;

    @SerializedName("lng")
    private final double lng;

    @Nullable
    @SerializedName("address")
    private final String address;

    @Nullable
    @SerializedName("description")
    private final String description;

    @SerializedName("people_count")
    private final int peopleCount;

    @SerializedName("urgency_level")
    private final String urgencyLevel;

    @Nullable
    @SerializedName("image_url")
    private final String imageUrl;

    public CreateSosRequest(double lat,
                            double lng,
                            @Nullable String address,
                            @Nullable String description,
                            int peopleCount,
                            String urgencyLevel,
                            @Nullable String imageUrl) {
        this.lat = lat;
        this.lng = lng;
        this.address = address;
        this.description = description;
        this.peopleCount = peopleCount;
        this.urgencyLevel = urgencyLevel;
        this.imageUrl = imageUrl;
    }
}
