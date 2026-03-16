package com.drc.aidbridge.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.drc.aidbridge.data.local.entity.AppSettingsEntity;
import com.drc.aidbridge.data.local.dao.AppSettingsDao;

/**
 * AppDatabase — the single Room database instance.
 *
 * IMPORTANT: Increment the version number whenever the schema changes.
 * Use proper Migration objects in DatabaseModule instead of fallbackToDestructiveMigration()
 * before production release.
 *
 * TODO: Add entity classes to the @Database annotation's entities array as they are created.
 */
@Database(
        entities = { AppSettingsEntity.class },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    // TODO: Add abstract DAO methods here as entities are added.
    public abstract AppSettingsDao appSettingsDao();
}
