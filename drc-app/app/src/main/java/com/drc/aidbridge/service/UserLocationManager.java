package com.drc.aidbridge.service;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.drc.aidbridge.utils.TokenManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class UserLocationManager {

    public static final long DEFAULT_FRESH_LOCATION_MAX_AGE_MS = 120_000L;
    public static final long QUICK_SOS_FRESH_LOCATION_MAX_AGE_MS = 15_000L;

    private static final long LOCATION_UPDATE_INTERVAL_MS = 15_000L;
    private static final long LOCATION_MIN_UPDATE_INTERVAL_MS = 8_000L;

    private final Context appContext;
    private final TokenManager tokenManager;
    private final FusedLocationProviderClient fusedLocationProviderClient;
    private final LocationRequest locationRequest;
    private final LocationCallback locationCallback;
    private final Set<LocationUpdateListener> locationUpdateListeners = new CopyOnWriteArraySet<>();

    @Nullable
    private LocationSnapshot latestLocation;

    private boolean tracking;

    @Inject
    public UserLocationManager(@ApplicationContext Context appContext,
                               TokenManager tokenManager) {
        this.appContext = appContext;
        this.tokenManager = tokenManager;
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(appContext);
        this.locationRequest = new LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(LOCATION_MIN_UPDATE_INTERVAL_MS)
            .build();
        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateLocation(location);
                }
            }
        };
        this.latestLocation = loadCachedLocation();
    }

    public void startForegroundTracking() {
        if (tracking) {
            refreshOnce();
            return;
        }

        if (!hasLocationPermission() || !isLocationProviderEnabled()) {
            return;
        }

        tracking = true;
        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            );
            refreshOnce();
        } catch (SecurityException ignored) {
            tracking = false;
        }
    }

    public void stopForegroundTracking() {
        if (!locationUpdateListeners.isEmpty()) {
            return;
        }

        tracking = false;
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    public boolean isTracking() {
        return tracking;
    }

    public void refreshOnce() {
        if (!hasLocationPermission() || !isLocationProviderEnabled()) {
            return;
        }

        try {
            CancellationTokenSource tokenSource = new CancellationTokenSource();
            fusedLocationProviderClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        updateLocation(location);
                        return;
                    }

                    requestLastKnownLocation();
                })
                .addOnFailureListener(ignored -> requestLastKnownLocation());
        } catch (SecurityException ignored) {
            // Permission state can change while the app is running.
        }
    }

    public void updateLocation(double latitude, double longitude) {
        updateLocation(latitude, longitude, null, System.currentTimeMillis());
    }

    public void updateLocation(double latitude, double longitude, long updatedAtMillis) {
        updateLocation(latitude, longitude, null, updatedAtMillis);
    }

    public void updateLocation(@Nullable Location location) {
        if (location == null) {
            return;
        }

        long capturedAtMillis = location.getTime() > 0L
            ? location.getTime()
            : System.currentTimeMillis();
        Double accuracy = location.hasAccuracy() ? (double) location.getAccuracy() : null;
        updateLocation(location.getLatitude(), location.getLongitude(), accuracy, capturedAtMillis);
    }

    public void updateLocation(double latitude,
                               double longitude,
                               @Nullable Double accuracy,
                               long updatedAtMillis) {
        if (!isCoordinateValid(latitude, longitude)) {
            return;
        }

        long safeUpdatedAt = updatedAtMillis > 0L ? updatedAtMillis : System.currentTimeMillis();
        latestLocation = new LocationSnapshot(latitude, longitude, safeUpdatedAt, accuracy);
        tokenManager.saveLastKnownLocation(latitude, longitude, safeUpdatedAt);
        notifyLocationUpdated(latestLocation);
    }

    @Nullable
    public LocationSnapshot getLatestLocation() {
        if (latestLocation != null) {
            return latestLocation;
        }

        latestLocation = loadCachedLocation();
        return latestLocation;
    }

    @Nullable
    public LocationSnapshot getFreshLocation(long maxAgeMs) {
        LocationSnapshot snapshot = getLatestLocation();
        if (snapshot == null) {
            return null;
        }

        if (maxAgeMs <= 0L || snapshot.isFresh(maxAgeMs)) {
            return snapshot;
        }

        return null;
    }

    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isLocationProviderEnabled() {
        LocationManager locationManager =
            (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void addLocationUpdateListener(@NonNull LocationUpdateListener listener) {
        locationUpdateListeners.add(listener);
    }

    public void removeLocationUpdateListener(@NonNull LocationUpdateListener listener) {
        locationUpdateListeners.remove(listener);
    }

    private void requestLastKnownLocation() {
        try {
            fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        updateLocation(location);
                    }
                });
        } catch (SecurityException ignored) {
            // Permission state can change while the app is running.
        }
    }

    @Nullable
    private LocationSnapshot loadCachedLocation() {
        Double latitude = tokenManager.getLastKnownLatitude();
        Double longitude = tokenManager.getLastKnownLongitude();
        long updatedAtMillis = tokenManager.getLastKnownLocationUpdatedAt();

        if (latitude == null || longitude == null || !isCoordinateValid(latitude, longitude)) {
            return null;
        }

        return new LocationSnapshot(latitude, longitude, updatedAtMillis);
    }

    private boolean isCoordinateValid(double latitude, double longitude) {
        return latitude >= -90.0d
            && latitude <= 90.0d
            && longitude >= -180.0d
            && longitude <= 180.0d;
    }

    private void notifyLocationUpdated(@NonNull LocationSnapshot snapshot) {
        for (LocationUpdateListener listener : locationUpdateListeners) {
            listener.onLocationUpdated(snapshot);
        }
    }

    public interface LocationUpdateListener {
        void onLocationUpdated(@NonNull LocationSnapshot snapshot);
    }

    public static final class LocationSnapshot {

        private final double latitude;
        private final double longitude;
        private final long updatedAtMillis;
        @Nullable
        private final Double accuracy;

        public LocationSnapshot(double latitude, double longitude, long updatedAtMillis) {
            this(latitude, longitude, updatedAtMillis, null);
        }

        public LocationSnapshot(double latitude,
                                double longitude,
                                long updatedAtMillis,
                                @Nullable Double accuracy) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.updatedAtMillis = updatedAtMillis;
            this.accuracy = accuracy;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public long getUpdatedAtMillis() {
            return updatedAtMillis;
        }

        public long getCapturedAtMillis() {
            return updatedAtMillis;
        }

        @Nullable
        public Double getAccuracy() {
            return accuracy;
        }

        public boolean isFresh(long maxAgeMs) {
            if (updatedAtMillis <= 0L) {
                return false;
            }

            return System.currentTimeMillis() - updatedAtMillis <= maxAgeMs;
        }
    }
}
