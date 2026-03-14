package com.drc.aidbridge.utils;

import android.content.SharedPreferences;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * TokenManager — wrapper around EncryptedSharedPreferences for JWT token lifecycle management.
 *
 * Responsibilities:
 * - saveTokens():       Stores both access and refresh tokens after a successful login/register.
 * - getAccessToken():   Retrieves the current access token for outgoing HTTP requests.
 * - getRefreshToken():  Retrieves the refresh token when the access token has expired (401).
 * - saveUserInfo():     Caches basic user metadata (role, id, name, email) locally.
 * - clearAll():         Wipes all tokens and user data on logout.
 * - isLoggedIn():       Quick check if a valid token is present.
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

    /**
     * Saves both JWT tokens after a successful authentication response.
     *
     * TODO API INTEGRATION: Call this inside AuthRepositoryImpl after parsing the real
     * AuthResponse from the server.
     */
    public void saveTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString(Constants.KEY_ACCESS_TOKEN, accessToken)
                .putString(Constants.KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    /** Returns the stored access token, or null if not logged in. */
    public String getAccessToken() {
        return prefs.getString(Constants.KEY_ACCESS_TOKEN, null);
    }

    /** Returns the stored refresh token, or null if none exists. */
    public String getRefreshToken() {
        return prefs.getString(Constants.KEY_REFRESH_TOKEN, null);
    }

    /** Returns true if a non-null access token is present (does NOT validate expiry). */
    public boolean isLoggedIn() {
        String token = getAccessToken();
        return token != null && !token.isEmpty();
    }

    /**
     * Caches user metadata locally so Profile and Home screens can display data
     * without a network call on every launch.
     *
     * TODO API INTEGRATION: Call this with the UserDto data from the real AuthResponse.
     */
    public void saveUserInfo(String userId, String userName, String email, String role) {
        prefs.edit()
                .putString(Constants.KEY_USER_ID,    userId)
                .putString(Constants.KEY_USER_NAME,  userName)
                .putString(Constants.KEY_USER_EMAIL, email)
                .putString(Constants.KEY_USER_ROLE,  role)
                .apply();
    }

    /** Returns the cached user role string (matches UserRole enum name). */
    public String getUserRole() {
        return prefs.getString(Constants.KEY_USER_ROLE, null);
    }

    /** Returns the cached user display name. */
    public String getUserName() {
        return prefs.getString(Constants.KEY_USER_NAME, null);
    }

    /** Returns the cached user email. */
    public String getUserEmail() {
        return prefs.getString(Constants.KEY_USER_EMAIL, null);
    }

    /**
     * Clears ALL stored tokens and user data.
     * Call this on logout or when a token refresh ultimately fails (session expired).
     */
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
