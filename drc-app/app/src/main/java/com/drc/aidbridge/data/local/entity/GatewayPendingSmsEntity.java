package com.drc.aidbridge.data.local.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "gateway_pending_sms",
    indices = {
        @Index(value = "client_request_id", unique = true),
        @Index(value = "status")
    }
)
public class GatewayPendingSmsEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "client_request_id")
    public String clientRequestId;

    @ColumnInfo(name = "sender_phone")
    public String senderPhone;

    public double latitude;

    public double longitude;

    @Nullable
    public Double accuracy;

    @ColumnInfo(name = "triggered_at_millis")
    public long triggeredAtMillis;

    @ColumnInfo(name = "location_captured_at_millis")
    public long locationCapturedAtMillis;

    @ColumnInfo(name = "people_count")
    public int peopleCount;

    @ColumnInfo(name = "quick_sos")
    public boolean quickSos;

    @ColumnInfo(name = "raw_message")
    public String rawMessage;

    @ColumnInfo(name = "received_at_gateway_millis")
    public long receivedAtGatewayMillis;

    public String status;

    @ColumnInfo(name = "retry_count")
    public int retryCount;

    @ColumnInfo(name = "server_sos_id")
    @Nullable
    public String serverSosId;

    @ColumnInfo(name = "created_at_millis")
    public long createdAtMillis;

    @ColumnInfo(name = "updated_at_millis")
    public long updatedAtMillis;

    @ColumnInfo(name = "last_error")
    @Nullable
    public String lastError;

    public GatewayPendingSmsEntity(String clientRequestId,
                                   String senderPhone,
                                   double latitude,
                                   double longitude,
                                   @Nullable Double accuracy,
                                   long triggeredAtMillis,
                                   long locationCapturedAtMillis,
                                   int peopleCount,
                                   boolean quickSos,
                                   String rawMessage,
                                   long receivedAtGatewayMillis,
                                   String status,
                                   int retryCount,
                                   long createdAtMillis,
                                   long updatedAtMillis,
                                   @Nullable String lastError) {
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
        this.status = status;
        this.retryCount = retryCount;
        this.createdAtMillis = createdAtMillis;
        this.updatedAtMillis = updatedAtMillis;
        this.lastError = lastError;
    }
}
