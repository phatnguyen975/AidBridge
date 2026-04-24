package com.drc.aidbridge.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.drc.aidbridge.data.local.dao.AppSettingsDao;
import com.drc.aidbridge.data.local.dao.PendingSosLocationUpdateDao;
import com.drc.aidbridge.data.local.dao.VictimHistoryDao;
import com.drc.aidbridge.data.local.entity.AppSettingsEntity;
import com.drc.aidbridge.data.local.entity.PendingSosLocationUpdateEntity;
import com.drc.aidbridge.data.local.entity.VictimHistoryEntity;

/**
 * AppDatabase — the single Room database instance.
 *
 * IMPORTANT: Increment the version number whenever the schema changes.
 * Use proper Migration objects in DatabaseModule instead of fallbackToDestructiveMigration()
 * before production release.
 *
 */
@Database(
    entities = {
        AppSettingsEntity.class,
        VictimHistoryEntity.class,
        PendingSosLocationUpdateEntity.class
    },
    version = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract AppSettingsDao appSettingsDao();

    public abstract VictimHistoryDao victimHistoryDao();

    public abstract PendingSosLocationUpdateDao pendingSosLocationUpdateDao();
}
