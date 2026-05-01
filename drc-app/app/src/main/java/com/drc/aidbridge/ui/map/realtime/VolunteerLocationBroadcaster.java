package com.drc.aidbridge.ui.map.realtime;

// import android.Manifest;
// import android.content.Context;
// import android.content.pm.PackageManager;
// import android.location.Location;
// import android.os.Looper;
// import android.util.Log;
// 
// import androidx.annotation.NonNull;
// import androidx.core.app.ActivityCompat;
// 
// import com.google.android.gms.location.FusedLocationProviderClient;
// import com.google.android.gms.location.LocationCallback;
// import com.google.android.gms.location.LocationRequest;
// import com.google.android.gms.location.LocationResult;
// import com.google.android.gms.location.LocationServices;
// import com.google.android.gms.location.Priority;
// 
// /**
//  * Manager class that broadcasts the Volunteer's live location to a Supabase Realtime channel.
//  * 
//  * Required Manifest Permissions:
//  * - android.permission.INTERNET
//  * - android.permission.ACCESS_FINE_LOCATION
//  * - android.permission.ACCESS_COARSE_LOCATION
//  */
// public class VolunteerLocationBroadcaster {
// 
//     private static final String TAG = "VolunteerLocationBroadcaster";
//     
//     private final Context context;
//     private final String missionId;
//     private final SupabaseBroadcastHelper broadcastHelper;
//     
//     private FusedLocationProviderClient fusedLocationClient;
//     private LocationCallback locationCallback;
//     private boolean isBroadcasting = false;
// 
//     public VolunteerLocationBroadcaster(@NonNull Context context, 
//                                         @NonNull String supabaseUrl, 
//                                         @NonNull String supabaseAnonKey, 
//                                         @NonNull String missionId) {
//         this.context = context.getApplicationContext();
//         this.missionId = missionId;
//         this.broadcastHelper = new SupabaseBroadcastHelper(supabaseUrl, supabaseAnonKey);
//     }
// 
//     public void startBroadcasting() {
//         if (isBroadcasting) {
//             return;
//         }
// 
//         Log.d(TAG, "Starting location broadcasting for mission: " + missionId);
// 
//         broadcastHelper.joinChannel(missionId, () -> {
//             Log.d(TAG, "Supabase channel connected, starting GPS tracking.");
//             initLocationUpdates();
//         });
//         
//         isBroadcasting = true;
//     }
// 
//     private void initLocationUpdates() {
//         if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && 
//             ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//             Log.e(TAG, "Permissions not granted for location updates.");
//             return;
//         }
// 
//         fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
// 
//         // Configure location requests for a 5-second refresh interval
//         LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
//                 .setMinUpdateIntervalMillis(3000L)
//                 .build();
// 
//         locationCallback = new LocationCallback() {
//             @Override
//             public void onLocationResult(@NonNull LocationResult locationResult) {
//                 Location location = locationResult.getLastLocation();
//                 if (location != null) {
//                     float bearing = location.hasBearing() ? location.getBearing() : 0.0f;
//                     broadcastHelper.broadcastLocation(
//                             location.getLatitude(), 
//                             location.getLongitude(), 
//                             bearing
//                     );
//                 }
//             }
//         };
// 
//         try {
//             fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
//         } catch (SecurityException e) {
//             Log.e(TAG, "SecurityException when requesting location updates", e);
//         }
//     }
// 
//     public void stopBroadcasting() {
//         if (!isBroadcasting) {
//             return;
//         }
// 
//         Log.d(TAG, "Stopping location broadcasting for mission: " + missionId);
// 
//         if (fusedLocationClient != null && locationCallback != null) {
//             try {
//                 fusedLocationClient.removeLocationUpdates(locationCallback);
//             } catch (Exception e) {
//                 Log.e(TAG, "Error removing location updates", e);
//             }
//         }
// 
//         broadcastHelper.disconnect();
//         isBroadcasting = false;
//     }
// }
