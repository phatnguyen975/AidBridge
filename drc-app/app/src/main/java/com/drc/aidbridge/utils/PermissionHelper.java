package com.drc.aidbridge.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * PermissionHelper — utility class for runtime permission checking and requesting.
 */
public final class PermissionHelper {

    private PermissionHelper() {
    }

    public static final int REQUEST_LOCATION = 1001;
    public static final int REQUEST_CAMERA = 1002;
    public static final int REQUEST_NOTIFY = 1003;

    // --- LOCATION ---

    /** Returns true if ACCESS_FINE_LOCATION permission has been granted. */
    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /** Requests ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permissions from the user. */
    public static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_LOCATION);
    }

    // --- CAMERA ---

    /** Returns true if CAMERA permission has been granted. */
    public static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /** Requests CAMERA permission from the user. */
    public static void requestCameraPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA);
    }

     // --- NOTIFICATION ---

    /** Returns true if POST_NOTIFICATIONS permission has been granted. */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /** Requests POST_NOTIFICATIONS permission from the user. */
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFY);
        }
    }
}
