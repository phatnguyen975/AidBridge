package com.drc.aidbridge.di;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import com.drc.aidbridge.utils.Constants;
import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * AppModule — provides application-level singletons.
 * This includes the application Context, EncryptedSharedPreferences for secure
 * JWT token storage, and any other context-dependent utilities.
 */
@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    /**
     * Provides an EncryptedSharedPreferences instance backed by AES-256 GCM.
     * This is used by TokenManager for secure JWT access/refresh token storage.
     *
     * NOTE: If the device does not support AES-256-GCM or the keystore is
     * corrupted,
     * this will throw a GeneralSecurityException. The catch block falls back to
     * standard SharedPreferences.
     */
    @Provides
    @Singleton
    public SharedPreferences provideEncryptedSharedPreferences(@ApplicationContext Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    Constants.PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            e.printStackTrace();
            // Keystore or EncryptedSharedPreferences file is corrupted. Clear it and retry.
            try {
                context.deleteSharedPreferences(Constants.PREFS_NAME);

                MasterKey masterKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                return EncryptedSharedPreferences.create(
                        context,
                        Constants.PREFS_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            } catch (Exception ex) {
                ex.printStackTrace();
                return context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
            }
        }
    }
}
