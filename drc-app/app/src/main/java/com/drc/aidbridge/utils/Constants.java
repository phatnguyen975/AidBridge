package com.drc.aidbridge.utils;

import com.drc.aidbridge.BuildConfig;

/**
 * Constants — central file for all magic numbers, keys, and configuration values.
 */
public final class Constants {

    private Constants() {
    }

    // --- Network ---
    public static final String BASE_URL = BuildConfig.BASE_URL;
    public static final int CONNECT_TIMEOUT = 30;
    public static final int READ_TIMEOUT = 30;
    public static final int WRITE_TIMEOUT = 30;

    // --- SharedPreferences / EncryptedSharedPreferences ---
    public static final String PREF_NAME = "aidbridge_secure_prefs";
    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_REFRESH_TOKEN = "refresh_token";
    public static final String KEY_USER_ROLE = "user_role";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_USER_EMAIL = "user_email";

    // --- Room Database ---
    public static final String DB_NAME = "aidbridge_db";

    // --- OTP Verification ---
    public static final int OTP_LENGTH = 6;
    public static final int OTP_RESEND_COUNTDOWN = 60;

    // --- Splash Screen ---
    public static final int SPLASH_DELAY_MS = 2000;
}
