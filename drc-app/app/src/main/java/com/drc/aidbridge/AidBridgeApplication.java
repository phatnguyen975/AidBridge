package com.drc.aidbridge;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class AidBridgeApplication extends Application {

    public static final String FCM_CHANNEL_ID = "aidbridge_notifications_default";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "AidBridge Notifications";
            String description = "Thông báo quan trọng từ hệ thống cứu trợ";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(FCM_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
