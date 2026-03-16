package com.drc.aidbridge.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.drc.aidbridge.data.local.entity.AppSettingsEntity;

/**
 * AppSettingsDao — Data Access Object for the app_settings table.
 */
@Dao
public interface AppSettingsDao {

    /**
     * Inserts or updates the app settings.
     * @param settings The AppSettingsEntity object to save.
     * @OnConflictStrategy.REPLACE: This is the magic of the "Single-Row Pattern". 
     * If a row with id=1 already exists, Room will automatically DELETE the old row 
     * and INSERT this new one (effectively an UPSERT operation).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveSettings(AppSettingsEntity settings);

    /**
     * Retrieves the single configuration row.
     * @return The AppSettingsEntity, or null if it hasn't been created yet.
     */
    @Query("SELECT * FROM app_settings WHERE id = 1")
    AppSettingsEntity getSettings();
}
