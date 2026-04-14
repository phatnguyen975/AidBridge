package com.drc.aidbridge;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class AidBridgeApplication extends Application {

    public static final String FCM_CHANNEL_ID = "aidbridge_notifications_default";
    public static final String FCM_CHANNEL_EMERGENCY = "emergency";
    public static final String FCM_CHANNEL_UPDATES = "updates";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }

        NotificationChannel defaultChannel = new NotificationChannel(
                FCM_CHANNEL_ID,
                "AidBridge Notifications",
                NotificationManager.IMPORTANCE_HIGH
        );
        defaultChannel.setDescription("Thong bao quan trong tu he thong cuu tro");

        NotificationChannel emergencyChannel = new NotificationChannel(
                FCM_CHANNEL_EMERGENCY,
                "Emergency Dispatch",
                NotificationManager.IMPORTANCE_HIGH
        );
        emergencyChannel.setDescription("Thong bao dieu phoi khan cap cho tinh nguyen vien");
        emergencyChannel.enableVibration(true);

        NotificationChannel updatesChannel = new NotificationChannel(
                FCM_CHANNEL_UPDATES,
                "Mission Updates",
                NotificationManager.IMPORTANCE_HIGH
        );
        updatesChannel.setDescription("Thong bao dieu phoi va cap nhat nhiem vu");
        updatesChannel.enableVibration(true);

        notificationManager.createNotificationChannel(defaultChannel);
        notificationManager.createNotificationChannel(emergencyChannel);
        notificationManager.createNotificationChannel(updatesChannel);
    }
}
