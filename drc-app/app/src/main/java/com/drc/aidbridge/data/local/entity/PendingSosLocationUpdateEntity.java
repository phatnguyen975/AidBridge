package com.drc.aidbridge.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_sos_location_updates")
public class PendingSosLocationUpdateEntity {

    @PrimaryKey
    @NonNull
    public String sosId;

    public double latitude;

    public double longitude;

    @Nullable
    public Double accuracy;

    public long capturedAtMillis;

    public long updatedAtMillis;

    public PendingSosLocationUpdateEntity(@NonNull String sosId,
                                          double latitude,
                                          double longitude,
                                          @Nullable Double accuracy,
                                          long capturedAtMillis,
                                          long updatedAtMillis) {
        this.sosId = sosId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.capturedAtMillis = capturedAtMillis;
        this.updatedAtMillis = updatedAtMillis;
    }
}
