package com.drc.aidbridge.utils;

/**
 * Constants — central file for all magic numbers, keys, and configuration values.
 */
public final class Constants {

    private Constants() {
    }

    public static final String BASE_URL = BuildConfig.BASE_URL;

    public static final String PREF_NAME = "aidbridge_secure_prefs";
    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_REFRESH_TOKEN = "refresh_token";
    public static final String KEY_USER_ROLE = "user_role";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_USER_EMAIL = "user_email";

    public static final int CONNECT_TIMEOUT_SECONDS = 30;
    public static final int READ_TIMEOUT_SECONDS = 30;
    public static final int WRITE_TIMEOUT_SECONDS = 30;
    public static final int OTP_LENGTH = 6;
    public static final int OTP_RESEND_SECONDS = 60;
}
