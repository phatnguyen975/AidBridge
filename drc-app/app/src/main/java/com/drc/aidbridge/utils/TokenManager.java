package com.drc.aidbridge.utils;

import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * TokenManager — wrapper around EncryptedSharedPreferences for JWT token lifecycle management.
 * 
 * Injected via Hilt (@Singleton) — the SharedPreferences is an EncryptedSharedPreferences
 * instance provided by AppModule.
 */
@Singleton
public class TokenManager {

    private final SharedPreferences prefs;

    @Inject
    public TokenManager(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    /** Saves both JWT tokens after a successful authentication response. */
    public void saveTokens(@Nullable String accessToken, @Nullable String refreshToken) {
        SharedPreferences.Editor editor = prefs.edit();

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

    /** Returns the stored access token, or null if not logged in. */
    @Nullable
    public String getAccessToken() {
        return prefs.getString(Constants.KEY_ACCESS_TOKEN, null);
    }

    /** Returns the stored refresh token, or null if none exists. */
    @Nullable
    public String getRefreshToken() {
        return prefs.getString(Constants.KEY_REFRESH_TOKEN, null);
    }

    /**
     * Caches user metadata locally so Profile and Home screens can display data
     * without a network call on every launch.
     */
    public void saveUserInfo(String userId, String userName, String email, String role) {
        prefs.edit()
                .putString(Constants.KEY_USER_ID, userId)
                .putString(Constants.KEY_USER_NAME, userName)
                .putString(Constants.KEY_USER_EMAIL, email)
                .putString(Constants.KEY_USER_ROLE, role)
                .apply();
    }

    /** Returns the cached user role string (matches UserRole enum name). */
    public String getUserRole() {
        return prefs.getString(Constants.KEY_USER_ROLE, null);
    }

    /** Returns the cached user name. */
    public String getUserName() {
        return prefs.getString(Constants.KEY_USER_NAME, null);
    }

    /** Returns the cached user email. */
    public String getUserEmail() {
        return prefs.getString(Constants.KEY_USER_EMAIL, null);
    }

    /** Returns true if a non-null, non-blank access token is present (does NOT validate expiry). */
    public boolean hasActiveSession() {
        String accessToken = getAccessToken();
        String refreshToken = getRefreshToken();
        return accessToken != null
                && !accessToken.isBlank()
                && refreshToken != null
                && !refreshToken.isBlank();
    }

    /** Clears ALL stored tokens. */
    public void clearTokens() {
        prefs.edit()
                .remove(Constants.KEY_ACCESS_TOKEN)
                .remove(Constants.KEY_REFRESH_TOKEN)
                .apply();
    }

    /**
     * Clears ALL stored tokens and user data.
     * Call this on logout or when a token refresh ultimately fails (session expired).
     */
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
