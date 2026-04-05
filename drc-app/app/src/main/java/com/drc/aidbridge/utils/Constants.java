package com.drc.aidbridge.utils;

import com.drc.aidbridge.BuildConfig;
import java.util.regex.Pattern;

/**
 * Constants — central file for all magic numbers, keys, and configuration
 * values.
 */
public final class Constants {

    private Constants() {
    }

    // === Network ===
    public static final String BASE_URL = BuildConfig.BASE_URL;
    public static final int CONNECT_TIMEOUT = 30;
    public static final int READ_TIMEOUT = 30;
    public static final int WRITE_TIMEOUT = 30;

    // === Auth Endpoints ===
    public static final String AUTH_PATH_PREFIX = "/auth/";
    public static final String REFRESH_TOKEN_ENDPOINT = "auth/refresh";

    // === SharedPreferences / EncryptedSharedPreferences ===
    public static final String PREFS_NAME = "aidbridge_secure_prefs";
    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_REFRESH_TOKEN = "refresh_token";
    public static final String KEY_USER_ROLE = "user_role";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_USER_EMAIL = "user_email";
    public static final String KEY_USER_PHONE = "user_phone";
    public static final String KEY_USER_AVATAR = "user_avatar";
    public static final String KEY_USER_ADDRESS = "user_address";
    public static final String KEY_USER_VERIFIED = "user_verified";
    public static final String KEY_FCM_TOKEN = "fcm_token";

    // === Room Database ===
    public static final String DB_NAME = "aidbridge_db";

    // === Splash ===
    public static final int SPLASH_DELAY_MS = 2000;

    // === Navigation ===
    public static final long NAVIGATE_DEBOUNCE_MS = 500L;

    // === Auth Validation ===
    public static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    public static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^[A-Za-z0-9]{6,}$");
    public static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\d{10}$");
    public static final Pattern OTP_PATTERN = Pattern.compile(
            "^\\d{6}$");
    public static final int OTP_COUNTDOWN_SEC = 60;
}
