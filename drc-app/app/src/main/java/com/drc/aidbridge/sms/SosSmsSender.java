package com.drc.aidbridge.sms;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class SosSmsSender {

    public interface Callback {
        void onResult(@NonNull SmsSendResult result);
    }

    public static class SmsSendResult {
        private final boolean success;
        private final String errorMessage;
        private final boolean permissionMissing;

        private SmsSendResult(boolean success, String errorMessage, boolean permissionMissing) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.permissionMissing = permissionMissing;
        }

        public static SmsSendResult success() {
            return new SmsSendResult(true, "", false);
        }

        public static SmsSendResult failed(String errorMessage) {
            return new SmsSendResult(false, errorMessage, false);
        }

        public static SmsSendResult permissionMissing() {
            return new SmsSendResult(false, "SEND_SMS permission is missing", true);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isPermissionMissing() {
            return permissionMissing;
        }
    }

    private static final String ACTION_SMS_SENT_PREFIX = "com.drc.aidbridge.SOS_SMS_SENT.";
    private static final long SMS_CALLBACK_TIMEOUT_MS = 30_000L;

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Inject
    public SosSmsSender(@ApplicationContext Context appContext) {
        this.appContext = appContext.getApplicationContext();
    }

    public boolean canAttemptSms() {
        return appContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING);
    }

    public boolean hasSendSmsPermission() {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.SEND_SMS)
            == PackageManager.PERMISSION_GRANTED;
    }

    public void send(@NonNull String destination,
                     @NonNull String body,
                     @NonNull String clientRequestId,
                     @NonNull Callback callback) {
        String safeDestination = destination.trim();
        if (safeDestination.isEmpty()) {
            callback.onResult(SmsSendResult.failed("SOS gateway phone number is not configured"));
            return;
        }

        if (isEmulatorConsoleNumber(safeDestination) && !isRunningOnEmulator()) {
            callback.onResult(SmsSendResult.failed(
                "So gateway " + safeDestination + " chi dung giua cac Android Emulator. May that can so SIM that cua thiet bi staff gateway."
            ));
            return;
        }

        if (!canAttemptSms()) {
            callback.onResult(SmsSendResult.failed("Device does not support SMS messaging"));
            return;
        }

        if (!hasSendSmsPermission()) {
            callback.onResult(SmsSendResult.permissionMissing());
            return;
        }

        String action = ACTION_SMS_SENT_PREFIX + Math.abs(clientRequestId.hashCode()) + "." + System.currentTimeMillis();
        Intent sentIntent = new Intent(action);
        sentIntent.setPackage(appContext.getPackageName());

        ArrayList<String> parts;
        try {
            SmsManager smsManager = SmsManager.getDefault();
            parts = smsManager.divideMessage(body);
            if (parts == null || parts.isEmpty()) {
                callback.onResult(SmsSendResult.failed("SMS body is empty"));
                return;
            }

            sendWithCallbacks(smsManager, safeDestination, body, parts, sentIntent, action, callback);
        } catch (Exception exception) {
            callback.onResult(SmsSendResult.failed(safeMessage(exception)));
        }
    }

    private void sendWithCallbacks(@NonNull SmsManager smsManager,
                                   @NonNull String destination,
                                   @NonNull String fullBody,
                                   @NonNull ArrayList<String> parts,
                                   @NonNull Intent sentIntent,
                                   @NonNull String action,
                                   @NonNull Callback callback) {
        AtomicInteger remaining = new AtomicInteger(parts.size());
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicBoolean finished = new AtomicBoolean(false);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (getResultCode() != Activity.RESULT_OK) {
                    failed.set(true);
                }
                if (remaining.decrementAndGet() <= 0) {
                    finish();
                }
            }

            private void finish() {
                if (!finished.compareAndSet(false, true)) {
                    return;
                }
                mainHandler.removeCallbacksAndMessages(action);
                unregisterQuietly(this);
                callback.onResult(failed.get()
                    ? SmsSendResult.failed("SMS provider reported send failure")
                    : SmsSendResult.success());
            }
        };

        registerReceiver(receiver, action);
        mainHandler.postDelayed(() -> {
            if (finished.compareAndSet(false, true)) {
                unregisterQuietly(receiver);
                callback.onResult(SmsSendResult.failed("SMS send callback timed out"));
            }
        }, SMS_CALLBACK_TIMEOUT_MS);

        ArrayList<PendingIntent> sentIntents = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            sentIntents.add(PendingIntent.getBroadcast(
                appContext,
                action.hashCode() + i,
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            ));
        }

        if (parts.size() == 1) {
            smsManager.sendTextMessage(destination, null, fullBody, sentIntents.get(0), null);
            return;
        }

        smsManager.sendMultipartTextMessage(destination, null, parts, sentIntents, null);
    }

    private void registerReceiver(@NonNull BroadcastReceiver receiver, @NonNull String action) {
        IntentFilter filter = new IntentFilter(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            appContext.registerReceiver(receiver, filter);
        }
    }

    private void unregisterQuietly(@NonNull BroadcastReceiver receiver) {
        try {
            appContext.unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable != null ? throwable.getMessage() : null;
        return message == null || message.trim().isEmpty() ? "SMS send failed" : message.trim();
    }

    private boolean isEmulatorConsoleNumber(@NonNull String destination) {
        return destination.matches("55\\d{2}");
    }

    private boolean isRunningOnEmulator() {
        String fingerprint = Build.FINGERPRINT != null ? Build.FINGERPRINT.toLowerCase() : "";
        String model = Build.MODEL != null ? Build.MODEL.toLowerCase() : "";
        String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER.toLowerCase() : "";
        String brand = Build.BRAND != null ? Build.BRAND.toLowerCase() : "";
        String device = Build.DEVICE != null ? Build.DEVICE.toLowerCase() : "";
        String product = Build.PRODUCT != null ? Build.PRODUCT.toLowerCase() : "";

        return fingerprint.startsWith("generic")
            || fingerprint.contains("emulator")
            || model.contains("google_sdk")
            || model.contains("emulator")
            || model.contains("android sdk built for")
            || manufacturer.contains("genymotion")
            || (brand.startsWith("generic") && device.startsWith("generic"))
            || product.contains("sdk")
            || product.contains("emulator");
    }
}
