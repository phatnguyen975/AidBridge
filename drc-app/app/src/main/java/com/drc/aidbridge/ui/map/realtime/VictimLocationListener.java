package com.drc.aidbridge.ui.map.realtime;

import android.util.Log;
import androidx.annotation.NonNull;

/**
 * Listener class used by the Victim's client to track the incoming real-time location of the Volunteer.
 * 
 * Required Manifest Permissions:
 * - android.permission.INTERNET
 */
public class VictimLocationListener {

    private static final String TAG = "VictimLocationListener";

    public interface OnLocationReceivedListener {
        void onLocationReceived(double latitude, double longitude, float heading);
    }

    public interface OnRouteReceivedListener {
        void onRouteReceived(java.util.List<org.osmdroid.util.GeoPoint> points);
    }

    private final String missionId;
    private final SupabaseBroadcastHelper broadcastHelper;
    private OnLocationReceivedListener locationListener;
    private OnRouteReceivedListener routeListener;
    private boolean isConnected = false;

    public VictimLocationListener(@NonNull String supabaseUrl, 
                                  @NonNull String supabaseAnonKey, 
                                  @NonNull String missionId) {
        this.missionId = missionId;
        this.broadcastHelper = new SupabaseBroadcastHelper(supabaseUrl, supabaseAnonKey);
    }

    /**
     * Starts listening for location updates from the Supabase Broadcast channel.
     */
    public void connect(@NonNull OnLocationReceivedListener locationListener, 
                        @NonNull OnRouteReceivedListener routeListener) {
        if (isConnected) {
            return;
        }

        this.locationListener = locationListener;
        this.routeListener = routeListener;
        Log.d(TAG, "Connecting to track mission: " + missionId);

        broadcastHelper.joinChannel(missionId, () -> {
            Log.d(TAG, "Joined tracking mission channel. Setting up event listeners.");
            
            // Setup location listener
            broadcastHelper.listenForLocationUpdates((latitude, longitude, heading) -> {
                if (this.locationListener != null) {
                    this.locationListener.onLocationReceived(latitude, longitude, heading);
                }
            });

            // Setup route listener
            broadcastHelper.listenForRouteUpdates(polyline -> {
                if (this.routeListener != null) {
                    java.util.List<org.osmdroid.util.GeoPoint> geoPoints = decodePolyline(polyline);
                    this.routeListener.onRouteReceived(geoPoints);
                }
            });
        });

        isConnected = true;
    }

    private java.util.List<org.osmdroid.util.GeoPoint> decodePolyline(String encoded) {
        java.util.List<org.osmdroid.util.GeoPoint> poly = new java.util.ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            org.osmdroid.util.GeoPoint p = new org.osmdroid.util.GeoPoint((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    /**
     * Unsubscribes from the Supabase Realtime channel and releases resources.
     */
    public void disconnect() {
        if (!isConnected) {
            return;
        }

        Log.d(TAG, "Disconnecting from mission tracking: " + missionId);
        broadcastHelper.disconnect();
        this.locationListener = null;
        this.routeListener = null;
        isConnected = false;
    }
}
