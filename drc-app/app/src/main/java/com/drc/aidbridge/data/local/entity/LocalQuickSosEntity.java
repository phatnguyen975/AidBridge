package com.drc.aidbridge.data.local.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "local_quick_sos",
    indices = {
        @Index(value = "client_request_id", unique = true),
        @Index(value = "status")
    }
)
public class LocalQuickSosEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "client_request_id")
    public String clientRequestId;

    public double latitude;

    public double longitude;

    @Nullable
    public Double accuracy;

    @ColumnInfo(name = "triggered_at_millis")
    public long triggeredAtMillis;

    @ColumnInfo(name = "location_captured_at_millis")
    public long locationCapturedAtMillis;

    @ColumnInfo(name = "device_info_json")
    public String deviceInfoJson;

    @ColumnInfo(name = "sms_body")
    public String smsBody;

    @ColumnInfo(name = "gateway_phone_number")
    public String gatewayPhoneNumber;

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

    public LocalQuickSosEntity(String clientRequestId,
                               double latitude,
                               double longitude,
                               @Nullable Double accuracy,
                               long triggeredAtMillis,
                               long locationCapturedAtMillis,
                               String deviceInfoJson,
                               String smsBody,
                               String gatewayPhoneNumber,
                               String status,
                               int retryCount,
                               long createdAtMillis,
                               long updatedAtMillis,
                               @Nullable String lastError) {
        this.clientRequestId = clientRequestId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.triggeredAtMillis = triggeredAtMillis;
        this.locationCapturedAtMillis = locationCapturedAtMillis;
        this.deviceInfoJson = deviceInfoJson;
        this.smsBody = smsBody;
        this.gatewayPhoneNumber = gatewayPhoneNumber;
        this.status = status;
        this.retryCount = retryCount;
        this.createdAtMillis = createdAtMillis;
        this.updatedAtMillis = updatedAtMillis;
        this.lastError = lastError;
    }
}
