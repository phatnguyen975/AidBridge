package com.drc.aidbridge.di;

import android.content.Context;
import androidx.room.Room;

import com.drc.aidbridge.data.local.AppDatabase;
import com.drc.aidbridge.data.local.dao.AppSettingsDao;
import com.drc.aidbridge.utils.Constants;

import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * DatabaseModule — provides the Room AppDatabase singleton and all DAO instances.
 * 
 * Room operates on a background thread; DAOs are injected directly into Repositories.
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    /**
     * Provides the Room AppDatabase singleton.
     * fallbackToDestructiveMigration() is used during development only.
     *
     * PRODUCTION: Replace fallbackToDestructiveMigration() with proper
     * Migration objects before releasing to end users to prevent data loss on schema updates.
     */
    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, Constants.DB_NAME)
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    @Singleton
    public AppSettingsDao provideAppSettingsDao(AppDatabase database) {
        return database.appSettingsDao();
    }
}
