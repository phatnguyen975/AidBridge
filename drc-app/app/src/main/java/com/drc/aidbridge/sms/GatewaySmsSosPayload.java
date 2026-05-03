package com.drc.aidbridge.sms;

import androidx.annotation.Nullable;

public class GatewaySmsSosPayload {

    private final String clientRequestId;
    private final String senderPhone;
    private final double latitude;
    private final double longitude;
    @Nullable
    private final Double accuracy;
    private final long triggeredAtMillis;
    private final long locationCapturedAtMillis;
    private final int peopleCount;
    private final boolean quickSos;
    private final String rawMessage;
    private final long receivedAtGatewayMillis;

    public GatewaySmsSosPayload(String clientRequestId,
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
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Nullable
    public Double getAccuracy() {
        return accuracy;
    }

    public long getTriggeredAtMillis() {
        return triggeredAtMillis;
    }

    public long getLocationCapturedAtMillis() {
        return locationCapturedAtMillis;
    }

    public int getPeopleCount() {
        return peopleCount;
    }

    public boolean isQuickSos() {
        return quickSos;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public long getReceivedAtGatewayMillis() {
        return receivedAtGatewayMillis;
    }
}
