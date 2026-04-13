package com.drc.aidbridge.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.drc.aidbridge.AidBridgeApplication;
import com.drc.aidbridge.R;
import com.drc.aidbridge.domain.repository.AuthRepository;
import com.drc.aidbridge.ui.main.MainActivity;
import com.drc.aidbridge.utils.Constants;
import com.drc.aidbridge.utils.TokenManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_Service";
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    @Inject
    TokenManager tokenManager;

    @Inject
    AuthRepository authRepository;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        String title = null;
        String body = null;
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        } else if (!remoteMessage.getData().isEmpty()) {
            title = remoteMessage.getData().get(Constants.EXTRA_NOTIFICATION_TITLE);
            body = remoteMessage.getData().get(Constants.EXTRA_NOTIFICATION_BODY);
        }

        if (body != null) {
            sendNotification(title, body, remoteMessage);
        }
    }

    @Override
    public void onNewToken(@NonNull String newToken) {
        super.onNewToken(newToken);
        String sanitizedToken = sanitize(newToken);
        if (sanitizedToken == null) {
            return;
        }

        tokenManager.saveFcmToken(sanitizedToken);
        if (!tokenManager.hasActiveSession()) {
            return;
        }

        String deviceId = sanitize(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
        if (deviceId != null) {
            backgroundExecutor.execute(() -> authRepository.updateFcmToken(deviceId, sanitizedToken));
        }
    }

    private void sendNotification(String title, String messageBody, @NonNull RemoteMessage remoteMessage) {
        String missionType = sanitize(remoteMessage.getData().get(Constants.EXTRA_MISSION_TYPE));
        int notificationId = resolveNotificationId(remoteMessage);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        attachDispatchExtras(intent, remoteMessage);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                this,
                resolveChannelId(remoteMessage, missionType)
        )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title != null ? title : getString(R.string.app_name))
                .setContentText(messageBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify(notificationId, notificationBuilder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Permission not granted for notifications", e);
        }
    }

    private void attachDispatchExtras(@NonNull Intent intent, @NonNull RemoteMessage remoteMessage) {
        if (remoteMessage.getData().isEmpty()) {
            return;
        }

        for (String key : new String[]{
                Constants.EXTRA_NOTIFICATION_TYPE,
                Constants.EXTRA_NOTIFICATION_TITLE,
                Constants.EXTRA_NOTIFICATION_BODY,
                Constants.EXTRA_MISSION_ID,
                Constants.EXTRA_DISPATCH_ATTEMPT_ID,
                Constants.EXTRA_MISSION_TYPE,
                Constants.EXTRA_DISPATCH_TYPE,
                Constants.EXTRA_EXPIRES_AT,
                Constants.EXTRA_CHANNEL_ID,
                Constants.EXTRA_CLICK_ACTION
        }) {
            String value = sanitize(remoteMessage.getData().get(key));
            if (value != null) {
                intent.putExtra(key, value);
            }
        }
    }

    @NonNull
    private String resolveChannelId(@NonNull RemoteMessage remoteMessage, String missionType) {
        String explicitChannelId = sanitize(remoteMessage.getData().get(Constants.EXTRA_CHANNEL_ID));
        if (explicitChannelId != null) {
            return explicitChannelId;
        }

        if ("RESCUE".equalsIgnoreCase(missionType)) {
            return AidBridgeApplication.FCM_CHANNEL_EMERGENCY;
        }

        if ("DELIVERY".equalsIgnoreCase(missionType)) {
            return AidBridgeApplication.FCM_CHANNEL_UPDATES;
        }

        return AidBridgeApplication.FCM_CHANNEL_ID;
    }

    private int resolveNotificationId(@NonNull RemoteMessage remoteMessage) {
        String dispatchAttemptId = sanitize(remoteMessage.getData().get(Constants.EXTRA_DISPATCH_ATTEMPT_ID));
        if (dispatchAttemptId != null) {
            return Math.abs(dispatchAttemptId.hashCode());
        }
        return (int) System.currentTimeMillis();
    }

    @Override
    public void onDestroy() {
        backgroundExecutor.shutdown();
        super.onDestroy();
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
