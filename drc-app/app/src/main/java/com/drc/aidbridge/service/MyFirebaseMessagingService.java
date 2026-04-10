package com.drc.aidbridge.service;

import android.app.PendingIntent;
import android.content.Context;
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

        // Ưu tiên lấy từ Notification Payload
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        } 
        // Nếu không có, lấy từ Data Payload (Server thường gửi dạng này để xử lý custom)
        else if (remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");
        }

        if (body != null) {
            sendNotification(title, body);
        }
    }

    @Override
    public void onNewToken(@NonNull String newToken) {
        super.onNewToken(newToken);
        String sanitizedToken = sanitize(newToken);
        if (sanitizedToken == null) return;

        tokenManager.saveFcmToken(sanitizedToken);
        if (!tokenManager.hasActiveSession()) return;

        String deviceId = sanitize(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
        if (deviceId != null) {
            backgroundExecutor.execute(() -> authRepository.updateFcmToken(deviceId, sanitizedToken));
        }
    }

    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
        // SỬ DỤNG FCM_CHANNEL_ID từ AidBridgeApplication
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, AidBridgeApplication.FCM_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title != null ? title : getString(R.string.app_name))
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        
        // Kiểm tra quyền trước khi gửi (Dành cho Android 13+)
        try {
            notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Permission not granted for notifications", e);
        }
    }

    @Override
    public void onDestroy() {
        backgroundExecutor.shutdown();
        super.onDestroy();
    }

    private String sanitize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
