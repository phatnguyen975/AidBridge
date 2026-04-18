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

    /** Caches user metadata locally after login for quick access throughout the app. */
    public void saveUserInfo(String userId,
                             String userName,
                             String email,
                             String phone,
                             String role,
                             String avatarUrl,
                             boolean verified) {
        prefs.edit()
                .putString(Constants.KEY_USER_ID, userId)
                .putString(Constants.KEY_USER_NAME, userName)
                .putString(Constants.KEY_USER_EMAIL, email)
                .putString(Constants.KEY_USER_PHONE, phone)
                .putString(Constants.KEY_USER_ROLE, role)
                .putString(Constants.KEY_USER_AVATAR, avatarUrl)
                .putBoolean(Constants.KEY_USER_VERIFIED, verified)
                .apply();
    }

    /** Stores the latest FCM token from Firebase. */
    public void saveFcmToken(@Nullable String fcmToken) {
        SharedPreferences.Editor editor = prefs.edit();
        if (fcmToken == null || fcmToken.isBlank()) {
            editor.remove(Constants.KEY_FCM_TOKEN);
        } else {
            editor.putString(Constants.KEY_FCM_TOKEN, fcmToken);
        }
        editor.apply();
    }

    /** Returns locally cached FCM token if available. */
    @Nullable
    public String getFcmToken() {
        return prefs.getString(Constants.KEY_FCM_TOKEN, null);
    }

    /** Returns the cached user role string (matches UserRole enum name). */
    public String getUserRole() {
        return prefs.getString(Constants.KEY_USER_ROLE, null);
    }

    /** Returns the cached user id. */
    public String getUserId() {
        return prefs.getString(Constants.KEY_USER_ID, null);
    }

    /** Returns the cached user name. */
    public String getUserName() {
        return prefs.getString(Constants.KEY_USER_NAME, null);
    }

    /** Returns the cached user email. */
    public String getUserEmail() {
        return prefs.getString(Constants.KEY_USER_EMAIL, null);
    }

    /** Returns the cached user phone number. */
    public String getUserPhone() {
        return prefs.getString(Constants.KEY_USER_PHONE, null);
    }

    /** Returns the cached avatar URL. */
    public String getUserAvatar() {
        return prefs.getString(Constants.KEY_USER_AVATAR, null);
    }

    /** Returns the cached address. */
    public String getUserAddress() {
        return prefs.getString(Constants.KEY_USER_ADDRESS, null);
    }

    public void saveLastKnownLocation(double latitude, double longitude, long updatedAtMillis) {
        prefs.edit()
            .putString(Constants.KEY_LAST_LOCATION_LAT, Double.toString(latitude))
            .putString(Constants.KEY_LAST_LOCATION_LNG, Double.toString(longitude))
            .putLong(Constants.KEY_LAST_LOCATION_UPDATED_AT, Math.max(0L, updatedAtMillis))
            .apply();
    }

    @Nullable
    public Double getLastKnownLatitude() {
        return parseNullableDouble(prefs.getString(Constants.KEY_LAST_LOCATION_LAT, null));
    }

    @Nullable
    public Double getLastKnownLongitude() {
        return parseNullableDouble(prefs.getString(Constants.KEY_LAST_LOCATION_LNG, null));
    }

    public long getLastKnownLocationUpdatedAt() {
        return prefs.getLong(Constants.KEY_LAST_LOCATION_UPDATED_AT, 0L);
    }

    public void saveActiveSosTrackingId(@Nullable String sosId) {
        SharedPreferences.Editor editor = prefs.edit();
        if (sosId == null || sosId.isBlank()) {
            editor.remove(Constants.KEY_ACTIVE_SOS_TRACKING_ID);
        } else {
            editor.putString(Constants.KEY_ACTIVE_SOS_TRACKING_ID, sosId);
        }
        editor.apply();
    }

    @Nullable
    public String getActiveSosTrackingId() {
        return prefs.getString(Constants.KEY_ACTIVE_SOS_TRACKING_ID, null);
    }

    public void clearActiveSosTrackingId() {
        prefs.edit().remove(Constants.KEY_ACTIVE_SOS_TRACKING_ID).apply();
    }

    /** Updates editable user fields after successful profile API calls. */
    public void updateUserInfo(String userName,
                               String phone,
                               String email,
                               String avatarUrl,
                               String address) {
        SharedPreferences.Editor editor = prefs.edit();

        if (userName != null) {
            editor.putString(Constants.KEY_USER_NAME, userName);
        }

        if (phone != null) {
            editor.putString(Constants.KEY_USER_PHONE, phone);
        }

        if (email != null) {
            editor.putString(Constants.KEY_USER_EMAIL, email);
        }

        if (avatarUrl != null) {
            if (avatarUrl.isBlank()) {
                editor.remove(Constants.KEY_USER_AVATAR);
            } else {
                editor.putString(Constants.KEY_USER_AVATAR, avatarUrl);
            }
        }

        if (address != null) {
            editor.putString(Constants.KEY_USER_ADDRESS, address);
        }

        editor.apply();
    }

    /** Returns the cached verified state of the current user. */
    public boolean isUserVerified() {
        return prefs.getBoolean(Constants.KEY_USER_VERIFIED, false);
    }

    /** Updates verified state for current cached user. */
    public void markUserAsVerified(boolean verified) {
        prefs.edit().putBoolean(Constants.KEY_USER_VERIFIED, verified).apply();
    }

    /** Clears ALL stored tokens. */
    public void clearTokens() {
        prefs.edit()
                .remove(Constants.KEY_ACCESS_TOKEN)
                .remove(Constants.KEY_REFRESH_TOKEN)
                .apply();
    }

    /** Clears ALL stored tokens and user data. */
    public void clearAll() {
        prefs.edit().clear().apply();
    }

    /** Checks if a valid (non-expired) refresh token exists, indicating an active session. */
    public boolean hasActiveSession() {
        String refreshToken = getRefreshToken();

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return false;
        }

        return !isTokenExpired(refreshToken);
    }

    /** Helper method to decode the JWT and check if the exp claim indicates expiration. */
    private boolean isTokenExpired(String token) {
        try {
            // JWT format: header.payload.signature (Base64URL-encoded)
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return true;
            }

            // Decode the payload (second part) to extract the exp claim
            String payload = new String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE));
            org.json.JSONObject jsonObject = new org.json.JSONObject(payload);

            // Find the exp field (expiration time in seconds)
            if (jsonObject.has("exp")) {
                long exp = jsonObject.getLong("exp");
                long currentTimeSeconds = System.currentTimeMillis() / 1000;
                // Allow a 5-minute (300 seconds) grace period to account for network latency
                return currentTimeSeconds >= (exp - 300);
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    @Nullable
    private Double parseNullableDouble(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
