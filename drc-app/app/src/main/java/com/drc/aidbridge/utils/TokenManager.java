package com.drc.aidbridge.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class TokenManager {

    private final SharedPreferences securePreferences;

    public TokenManager(@NonNull Context context) {
        securePreferences = createSecurePreferences(context.getApplicationContext());
    }

    private SharedPreferences createSecurePreferences(@NonNull Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    Constants.PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("Unable to initialize encrypted token storage.", exception);
        }
    }

    public void saveTokens(@Nullable String accessToken, @Nullable String refreshToken) {
        SharedPreferences.Editor editor = securePreferences.edit();

        if (accessToken == null || accessToken.isBlank()) {
            editor.remove(Constants.KEY_ACCESS_TOKEN);
        } else {
            editor.putString(Constants.KEY_ACCESS_TOKEN, accessToken);
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            editor.remove(Constants.KEY_REFRESH_TOKEN);
        } else {
            editor.putString(Constants.KEY_REFRESH_TOKEN, refreshToken);
        }

        editor.apply();
    }

    @Nullable
    public String getAccessToken() {
        return securePreferences.getString(Constants.KEY_ACCESS_TOKEN, null);
    }

    @Nullable
    public String getRefreshToken() {
        return securePreferences.getString(Constants.KEY_REFRESH_TOKEN, null);
    }

    public boolean hasActiveSession() {
        String accessToken = getAccessToken();
        String refreshToken = getRefreshToken();
        return accessToken != null
                && !accessToken.isBlank()
                && refreshToken != null
                && !refreshToken.isBlank();
    }

    public void clearTokens() {
        securePreferences.edit()
                .remove(Constants.KEY_ACCESS_TOKEN)
                .remove(Constants.KEY_REFRESH_TOKEN)
                .apply();
    }

    public void clearAll() {
        securePreferences.edit().clear().apply();
    }
}
