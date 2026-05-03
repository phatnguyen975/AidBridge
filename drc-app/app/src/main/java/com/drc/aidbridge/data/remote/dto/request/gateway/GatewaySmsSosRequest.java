package com.drc.aidbridge.data.remote.dto.request.gateway;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class GatewaySmsSosRequest {

    @SerializedName("clientRequestId")
    private final String clientRequestId;

    @SerializedName("senderPhone")
    private final String senderPhone;

    @SerializedName("latitude")
    private final double latitude;

    @SerializedName("longitude")
    private final double longitude;

    @Nullable
    @SerializedName("accuracy")
    private final Double accuracy;

    @SerializedName("triggeredAtMillis")
    private final long triggeredAtMillis;

    @SerializedName("locationCapturedAtMillis")
    private final long locationCapturedAtMillis;

    @SerializedName("peopleCount")
    private final int peopleCount;

    @SerializedName("quickSos")
    private final boolean quickSos;

    @SerializedName("rawMessage")
    private final String rawMessage;

    @SerializedName("receivedAtGatewayMillis")
    private final long receivedAtGatewayMillis;

    @SerializedName("source")
    private final String source;

    public GatewaySmsSosRequest(String clientRequestId,
                                String senderPhone,
                                double latitude,
                                double longitude,
                                @Nullable Double accuracy,
                                long triggeredAtMillis,
                                long locationCapturedAtMillis,
                                int peopleCount,
                                boolean quickSos,
                                String rawMessage,
                                long receivedAtGatewayMillis) {
        this.clientRequestId = clientRequestId;
        this.senderPhone = senderPhone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.triggeredAtMillis = triggeredAtMillis;
        this.locationCapturedAtMillis = locationCapturedAtMillis;
        this.peopleCount = peopleCount;
        this.quickSos = quickSos;
        this.rawMessage = rawMessage;
        this.receivedAtGatewayMillis = receivedAtGatewayMillis;
        this.source = "SMS";
    }
}
