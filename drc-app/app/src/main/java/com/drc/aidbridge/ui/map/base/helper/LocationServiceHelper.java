package com.drc.aidbridge.ui.map.base.helper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import org.osmdroid.util.GeoPoint;

public class LocationServiceHelper {

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 7024;

    public interface LocationListener {
        void onLocationChanged(@NonNull GeoPoint point);
        void onPermissionDenied();
    }

    @Nullable
    private FusedLocationProviderClient fusedLocationClient;
    @Nullable
    private LocationCallback locationCallback;
    @Nullable
    private LocationRequest locationRequest;
    @Nullable
    private LocationListener listener;
    
    private boolean isMapScreenActive = false;
    private boolean isSimulationRunning = false;

    public void init(@NonNull Activity activity, @NonNull LocationListener listener) {
        this.listener = listener;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);

        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateIntervalMillis(1500L)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!isMapScreenActive) {
                    return;
                }

                Location location = locationResult.getLastLocation();
                if (location == null || isSimulationRunning) {
                    return;
                }
                if (listener != null) {
                    listener.onLocationChanged(new GeoPoint(location.getLatitude(), location.getLongitude()));
                }
            }
        };
    }

    public void setMapScreenActive(boolean active) {
        this.isMapScreenActive = active;
    }

    public void setSimulationRunning(boolean running) {
        this.isSimulationRunning = running;
    }

    public void ensureLocation(@NonNull Activity activity) {
        if (!hasLocationPermission(activity)) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fetchInitialLocationImmediately();
        startLocationUpdates();
    }

    public void fetchInitialLocationImmediately() {
        if (fusedLocationClient == null) return;
        
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location == null || isSimulationRunning) {
                    return;
                }
                if (listener != null) {
                    listener.onLocationChanged(new GeoPoint(location.getLatitude(), location.getLongitude()));
                }
            });

            CancellationTokenSource tokenSource = new CancellationTokenSource();
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.getToken())
                    .addOnSuccessListener(location -> {
                        if (location == null || isSimulationRunning) {
                            return;
                        }
                        if (listener != null) {
                            listener.onLocationChanged(new GeoPoint(location.getLatitude(), location.getLongitude()));
                        }
                    });
        } catch (SecurityException ignored) {
        }
    }

    public void startLocationUpdates() {
        if (locationCallback == null || fusedLocationClient == null) {
            return;
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException ignored) {
        }
    }

    public void stopLocationUpdates() {
        if (locationCallback != null && fusedLocationClient != null) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            } catch (Exception ignored) {
            }
        }
    }

    public boolean hasLocationPermission(@NonNull Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        boolean granted = false;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                granted = true;
                break;
            }
        }

        if (!granted) {
            if (listener != null) {
                listener.onPermissionDenied();
            }
            return;
        }

        fetchInitialLocationImmediately();
        startLocationUpdates();
    }

    public void detach() {
        stopLocationUpdates();
        fusedLocationClient = null;
        locationCallback = null;
        locationRequest = null;
        listener = null;
    }
}
