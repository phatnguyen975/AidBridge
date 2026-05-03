package com.drc.aidbridge.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.drc.aidbridge.AidBridgeApplication;
import com.drc.aidbridge.R;
import com.drc.aidbridge.data.local.dao.PendingSosLocationUpdateDao;
import com.drc.aidbridge.data.local.entity.PendingSosLocationUpdateEntity;
import com.drc.aidbridge.data.remote.api.victim.SosApiService;
import com.drc.aidbridge.data.remote.dto.request.victim.UpdateRequestLocationRequest;
import com.drc.aidbridge.data.remote.dto.response.BaseResponse;
import com.drc.aidbridge.ui.main.MainActivity;
import com.drc.aidbridge.utils.NetworkUtils;
import com.drc.aidbridge.utils.TokenManager;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class EmergencyTrackingService extends Service {

    private static final String ACTION_START = "com.drc.aidbridge.service.action.START_EMERGENCY_TRACKING";
    private static final String ACTION_STOP = "com.drc.aidbridge.service.action.STOP_EMERGENCY_TRACKING";
    private static final String EXTRA_SOS_ID = "extra_sos_id";

    private static final int NOTIFICATION_ID = 4101;
    private static final long FLUSH_INTERVAL_MS = 15_000L;
    private static final float SIGNIFICANT_DISTANCE_METERS = 15.0f;

    @Inject
    UserLocationManager userLocationManager;

    @Inject
    PendingSosLocationUpdateDao pendingSosLocationUpdateDao;

    @Inject
    SosApiService sosApiService;

    @Inject
    TokenManager tokenManager;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private final Runnable periodicFlushRunnable = new Runnable() {
        @Override
        public void run() {
            flushPendingNow();
            scheduleNextFlush();
        }
    };

    private final UserLocationManager.LocationUpdateListener locationUpdateListener =
        this::handleLocationUpdate;

    @Nullable
    private String activeSosId;

    @Nullable
    private UserLocationManager.LocationSnapshot lastQueuedSnapshot;

    private boolean listenerRegistered;
    private boolean flushInFlight;

    public static void startTracking(@NonNull Context context, @NonNull String sosId) {
        Intent intent = new Intent(context, EmergencyTrackingService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_SOS_ID, sosId);
        ContextCompat.startForegroundService(context.getApplicationContext(), intent);
    }

    public static void stopTracking(@NonNull Context context) {
        Intent intent = new Intent(context, EmergencyTrackingService.class);
        intent.setAction(ACTION_STOP);
        context.getApplicationContext().startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            stopCurrentTracking(true);
            return START_NOT_STICKY;
        }

        String requestedSosId = sanitize(intent != null ? intent.getStringExtra(EXTRA_SOS_ID) : null);
        if (requestedSosId == null) {
            requestedSosId = sanitize(tokenManager.getActiveSosTrackingId());
        }

        if (requestedSosId == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startTrackingInternal(requestedSosId);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacks(periodicFlushRunnable);
        unregisterLocationListener();
        userLocationManager.stopForegroundTracking();
        ioExecutor.shutdown();
        super.onDestroy();
    }

    private void startTrackingInternal(@NonNull String sosId) {
        activeSosId = sosId;
        tokenManager.saveActiveSosTrackingId(sosId);
        lastQueuedSnapshot = userLocationManager.getLatestLocation();
        registerLocationListenerIfNeeded();
        userLocationManager.startForegroundTracking();
        userLocationManager.refreshOnce();
        flushPendingNow();
        scheduleNextFlush();
    }

    private void stopCurrentTracking(boolean clearPending) {
        mainHandler.removeCallbacks(periodicFlushRunnable);
        unregisterLocationListener();
        userLocationManager.stopForegroundTracking();

        String sosIdToClear = activeSosId != null ? activeSosId : tokenManager.getActiveSosTrackingId();
        activeSosId = null;
        lastQueuedSnapshot = null;
        tokenManager.clearActiveSosTrackingId();

        if (clearPending && sosIdToClear != null) {
            ioExecutor.execute(() -> pendingSosLocationUpdateDao.deleteBySosId(sosIdToClear));
        }

        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void registerLocationListenerIfNeeded() {
        if (listenerRegistered) {
            return;
        }

        userLocationManager.addLocationUpdateListener(locationUpdateListener);
        listenerRegistered = true;
    }

    private void unregisterLocationListener() {
        if (!listenerRegistered) {
            return;
        }

        userLocationManager.removeLocationUpdateListener(locationUpdateListener);
        listenerRegistered = false;
    }

    private void handleLocationUpdate(@NonNull UserLocationManager.LocationSnapshot snapshot) {
        String sosId = activeSosId;
        if (sosId == null || !hasSignificantChange(snapshot)) {
            return;
        }

        lastQueuedSnapshot = snapshot;
        long safeCapturedAtMillis = snapshot.getCapturedAtMillis() > 0L
            ? snapshot.getCapturedAtMillis()
            : System.currentTimeMillis();
        long queueUpdatedAtMillis = System.currentTimeMillis();

        PendingSosLocationUpdateEntity entity = new PendingSosLocationUpdateEntity(
            sosId,
            snapshot.getLatitude(),
            snapshot.getLongitude(),
            snapshot.getAccuracy(),
            safeCapturedAtMillis,
            queueUpdatedAtMillis
        );

        ioExecutor.execute(() -> {
            pendingSosLocationUpdateDao.upsert(entity);
            if (NetworkUtils.isConnected(getApplicationContext())) {
                flushPendingInternal(sosId);
            }
        });
    }

    private boolean hasSignificantChange(@NonNull UserLocationManager.LocationSnapshot snapshot) {
        if (lastQueuedSnapshot == null) {
            return true;
        }

        float[] distanceResult = new float[1];
        Location.distanceBetween(
            lastQueuedSnapshot.getLatitude(),
            lastQueuedSnapshot.getLongitude(),
            snapshot.getLatitude(),
            snapshot.getLongitude(),
            distanceResult
        );

        return distanceResult[0] >= SIGNIFICANT_DISTANCE_METERS;
    }

    private void flushPendingNow() {
        String sosId = activeSosId;
        if (sosId == null) {
            return;
        }

        ioExecutor.execute(() -> flushPendingInternal(sosId));
    }

    private void flushPendingInternal(@NonNull String sosId) {
        if (flushInFlight || !NetworkUtils.isConnected(getApplicationContext())) {
            return;
        }

        PendingSosLocationUpdateEntity pending = pendingSosLocationUpdateDao.findBySosId(sosId);
        if (pending == null) {
            return;
        }

        flushInFlight = true;
        UpdateRequestLocationRequest request = new UpdateRequestLocationRequest(
            pending.latitude,
            pending.longitude,
            pending.accuracy,
            Instant.ofEpochMilli(resolveTimestamp(pending.capturedAtMillis)).toString()
        );

        sosApiService.updateSosLocation(sosId, request).enqueue(new Callback<BaseResponse<Object>>() {
            @Override
            public void onResponse(Call<BaseResponse<Object>> call,
                                   Response<BaseResponse<Object>> response) {
                ioExecutor.execute(() -> {
                    flushInFlight = false;
                    if (response.isSuccessful()) {
                        BaseResponse<Object> body = response.body();
                        if (body == null || body.isSuccess()) {
                            pendingSosLocationUpdateDao.deleteIfUnchanged(sosId, pending.updatedAtMillis);
                        }
                    }

                    if (activeSosId != null && activeSosId.equals(sosId)) {
                        flushPendingInternal(sosId);
                    }
                });
            }

            @Override
            public void onFailure(Call<BaseResponse<Object>> call, Throwable t) {
                ioExecutor.execute(() -> flushInFlight = false);
            }
        });
    }

    private void scheduleNextFlush() {
        mainHandler.removeCallbacks(periodicFlushRunnable);
        if (activeSosId == null) {
            return;
        }

        mainHandler.postDelayed(periodicFlushRunnable, FLUSH_INTERVAL_MS);
    }

    @NonNull
    private Notification buildNotification() {
        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, AidBridgeApplication.FCM_CHANNEL_EMERGENCY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("SOS location tracking is active")
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("AidBridge is updating your SOS location while emergency tracking is active."))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOnlyAlertOnce(true)
            .build();
    }

    private long resolveTimestamp(long timestampMillis) {
        return timestampMillis > 0L ? timestampMillis : System.currentTimeMillis();
    }

    @Nullable
    private String sanitize(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
