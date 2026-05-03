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

    @Nullable
    @SerializedName("quick_sos")
    private final Boolean quickSos;

    @Nullable
    @SerializedName("triggered_at")
    private final String triggeredAt;

    @Nullable
    @SerializedName("location_captured_at")
    private final String locationCapturedAt;

    @Nullable
    @SerializedName("accuracy")
    private final Double accuracy;

    @Nullable
    @SerializedName("client_request_id")
    private final String clientRequestId;

    @Nullable
    @SerializedName("device_info")
    private final String deviceInfo;

    public CreateSosRequest(double lat,
                            double lng,
                            @Nullable String address,
                            @Nullable String description,
                            int peopleCount,
                            String urgencyLevel,
                            @Nullable String imageUrl) {
        this(
            lat,
            lng,
            address,
            description,
            peopleCount,
            urgencyLevel,
            imageUrl,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public CreateSosRequest(double lat,
                            double lng,
                            @Nullable String address,
                            @Nullable String description,
                            int peopleCount,
                            String urgencyLevel,
                            @Nullable String imageUrl,
                            @Nullable Boolean quickSos,
                            @Nullable String triggeredAt,
                            @Nullable String locationCapturedAt,
                            @Nullable Double accuracy,
                            @Nullable String clientRequestId,
                            @Nullable String deviceInfo) {
        this.lat = lat;
        this.lng = lng;
        this.address = address;
        this.description = description;
        this.peopleCount = peopleCount;
        this.urgencyLevel = urgencyLevel;
        this.imageUrl = imageUrl;
        this.quickSos = quickSos;
        this.triggeredAt = triggeredAt;
        this.locationCapturedAt = locationCapturedAt;
        this.accuracy = accuracy;
        this.clientRequestId = clientRequestId;
        this.deviceInfo = deviceInfo;
    }

    public static CreateSosRequest createQuickSos(double lat,
                                                  double lng,
                                                  @Nullable Double accuracy,
                                                  @Nullable String triggeredAt,
                                                  @Nullable String locationCapturedAt,
                                                  @Nullable String clientRequestId,
                                                  @Nullable String deviceInfo) {
        return new CreateSosRequest(
            lat,
            lng,
            null,
            null,
            1,
            "CRITICAL",
            null,
            Boolean.TRUE,
            triggeredAt,
            locationCapturedAt,
            accuracy,
            clientRequestId,
            deviceInfo
        );
    }
}
