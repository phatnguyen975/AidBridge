package com.drc.aidbridge.utils;

/**
 * Constants — central file for all magic numbers, keys, and configuration values.
 */
public final class Constants {

    private Constants() {
    }

    // === Network ===
    public static final String BASE_URL = "http://10.0.2.2:8080/api/";
    public static final int CONNECT_TIMEOUT = 30;
    public static final int READ_TIMEOUT = 30;
    public static final int WRITE_TIMEOUT = 30;

    // === Auth endpoints ===
    public static final String AUTH_PATH_PREFIX = "/auth/";
    public static final String REFRESH_TOKEN_ENDPOINT = "auth/refresh-token";

    // === SharedPreferences / EncryptedSharedPreferences ===
    public static final String PREFS_NAME = "aidbridge_secure_prefs";
    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_REFRESH_TOKEN = "refresh_token";
    public static final String KEY_USER_ROLE = "user_role";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_USER_EMAIL = "user_email";

    // === Room Database ===
    public static final String DB_NAME = "aidbridge_db";

    // === OTP ===
    public static final int OTP_LENGTH = 6;
    public static final int OTP_COUNTDOWN_SEC = 60;

    // === Validation ===
    public static final int PASSWORD_MIN_LENGTH = 6;

    // === Splash ===
    public static final int SPLASH_DELAY_MS = 2000;

    // === Navigation ===
    public static final long NAVIGATE_DEBOUNCE_MS = 500L;
}
