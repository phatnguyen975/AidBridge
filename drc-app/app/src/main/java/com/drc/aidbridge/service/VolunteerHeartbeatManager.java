package com.drc.aidbridge.service;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.drc.aidbridge.data.remote.api.volunteer.VolunteerApiService;
import com.drc.aidbridge.data.remote.dto.request.volunteer.PingVolunteerHeartbeatRequest;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerProfileResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class VolunteerHeartbeatManager {

    private static final long HEARTBEAT_INTERVAL_MS = 60_000L;

    private final Context appContext;
    private final VolunteerApiService volunteerApiService;
    private final UserLocationManager userLocationManager;
    private final FusedLocationProviderClient fusedLocationProviderClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable heartbeatRunnable = this::dispatchHeartbeat;

    @Nullable
    private Double lastLatitude;

    @Nullable
    private Double lastLongitude;

    private boolean running;

    @Inject
    public VolunteerHeartbeatManager(@ApplicationContext Context appContext,
                                     VolunteerApiService volunteerApiService,
                                     UserLocationManager userLocationManager) {
        this.appContext = appContext;
        this.volunteerApiService = volunteerApiService;
        this.userLocationManager = userLocationManager;
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(appContext);
    }

    public void start(@Nullable Double latitude, @Nullable Double longitude) {
        updateLastKnownLocation(latitude, longitude);
        userLocationManager.startForegroundTracking();
        running = true;
        scheduleNextHeartbeat(0L);
    }

    public void stop() {
        running = false;
        mainHandler.removeCallbacks(heartbeatRunnable);
    }

    public boolean isRunning() {
        return running;
    }

    public void updateLastKnownLocation(@Nullable Double latitude, @Nullable Double longitude) {
        if (latitude == null || longitude == null) {
            return;
        }

        lastLatitude = latitude;
        lastLongitude = longitude;
    }

    private void dispatchHeartbeat() {
        if (!running) {
            return;
        }

        UserLocationManager.LocationSnapshot snapshot = userLocationManager.getFreshLocation(
            UserLocationManager.DEFAULT_FRESH_LOCATION_MAX_AGE_MS
        );
        if (snapshot != null && hasLocationPermission()) {
            pingWithCoordinates(snapshot.getLatitude(), snapshot.getLongitude());
            userLocationManager.refreshOnce();
            return;
        }

        if (!hasLocationPermission() || !isLocationProviderEnabled()) {
            scheduleNextHeartbeat(HEARTBEAT_INTERVAL_MS);
            return;
        }

        try {
            CancellationTokenSource tokenSource = new CancellationTokenSource();
            fusedLocationProviderClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        pingWithCoordinates(location.getLatitude(), location.getLongitude());
                        return;
                    }

                    requestLastKnownLocation();
                })
                .addOnFailureListener(ignored -> requestLastKnownLocation());
        } catch (SecurityException ignored) {
            scheduleNextHeartbeat(HEARTBEAT_INTERVAL_MS);
        }
    }

    private void requestLastKnownLocation() {
        try {
            fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        pingWithCoordinates(location.getLatitude(), location.getLongitude());
                        return;
                    }

                    pingWithCachedCoordinatesOrReschedule();
                })
                .addOnFailureListener(ignored -> pingWithCachedCoordinatesOrReschedule());
        } catch (SecurityException ignored) {
            pingWithCachedCoordinatesOrReschedule();
        }
    }

    private void pingWithCachedCoordinatesOrReschedule() {
        if (lastLatitude != null && lastLongitude != null) {
            pingWithCoordinates(lastLatitude, lastLongitude);
            return;
        }

        scheduleNextHeartbeat(HEARTBEAT_INTERVAL_MS);
    }

    private void pingWithCoordinates(double latitude, double longitude) {
        updateLastKnownLocation(latitude, longitude);
        userLocationManager.updateLocation(latitude, longitude);

        volunteerApiService.pingVolunteerHeartbeat(new PingVolunteerHeartbeatRequest(latitude, longitude))
            .enqueue(new Callback<VolunteerProfileResponse>() {
                @Override
                public void onResponse(Call<VolunteerProfileResponse> call,
                                       Response<VolunteerProfileResponse> response) {
                    scheduleNextHeartbeat(HEARTBEAT_INTERVAL_MS);
                }

                @Override
                public void onFailure(Call<VolunteerProfileResponse> call, Throwable t) {
                    scheduleNextHeartbeat(HEARTBEAT_INTERVAL_MS);
                }
            });
    }

    private void scheduleNextHeartbeat(long delayMs) {
        mainHandler.removeCallbacks(heartbeatRunnable);
        if (!running) {
            return;
        }

        mainHandler.postDelayed(heartbeatRunnable, delayMs);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isLocationProviderEnabled() {
        LocationManager locationManager =
            (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}
