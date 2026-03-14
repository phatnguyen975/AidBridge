package com.drc.aidbridge.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * PermissionHelper — utility class for runtime permission checking and requesting.
 *
 * Covers: FINE_LOCATION, CAMERA, and POST_NOTIFICATIONS (Android 13+)
 */
public final class PermissionHelper {

    private PermissionHelper() {
    }

    public static final int REQUEST_LOCATION   = 1001;
    public static final int REQUEST_CAMERA     = 1002;
    public static final int REQUEST_NOTIFY     = 1003;

    /** Returns true if ACCESS_FINE_LOCATION permission has been granted. */
    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /** Requests ACCESS_FINE_LOCATION from the user. */
    public static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_LOCATION);
    }

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
}
