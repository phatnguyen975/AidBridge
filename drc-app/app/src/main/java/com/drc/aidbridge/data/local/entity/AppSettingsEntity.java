package com.drc.aidbridge.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * AppSettingsEntity — represents the app's settings stored in a single-row table.
 * 
 * By hardcoding the primary key (id = 1), we ensure that this table only ever contains ONE row.
 * Any subsequent insertions will just overwrite this exact row, acting as a persistent key-value store
 * that is more robust than SharedPreferences for structured data.
 */
@Entity(tableName = "app_settings")
public class AppSettingsEntity {

    @PrimaryKey
    public int id = 1;

    public long lastSyncTimestamp = 0L;
}
