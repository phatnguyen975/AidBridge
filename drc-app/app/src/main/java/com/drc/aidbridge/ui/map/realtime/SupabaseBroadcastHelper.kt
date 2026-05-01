package com.drc.aidbridge.ui.map.realtime

// import io.github.jan.supabase.createSupabaseClient
// import io.github.jan.supabase.realtime.Realtime
// import io.github.jan.supabase.realtime.RealtimeChannel
// import io.github.jan.supabase.realtime.realtime
// import io.github.jan.supabase.realtime.broadcast
// import io.github.jan.supabase.realtime.channel
// import kotlinx.coroutines.CoroutineScope
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.SupervisorJob
// import kotlinx.coroutines.launch
// import kotlinx.coroutines.cancel
// import kotlinx.coroutines.withContext
// import kotlinx.serialization.json.JsonObject
// import kotlinx.serialization.json.buildJsonObject
// import kotlinx.serialization.json.put
// import kotlinx.serialization.json.doubleOrNull
// import kotlinx.serialization.json.floatOrNull
// import kotlinx.serialization.json.jsonPrimitive
// import android.util.Log
// 
// /**
//  * A Kotlin wrapper that facilitates accessing Supabase Realtime Broadcast functionality
//  * from Java code without needing knowledge of Kotlin Coroutines or Flow.
//  */
// class SupabaseBroadcastHelper(
//     private val supabaseUrl: String,
//     private val anonKey: String
// ) {
//     private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
//     private val supabase = createSupabaseClient(supabaseUrl, anonKey) {
//         install(Realtime)
//     }
//     private var currentChannel: RealtimeChannel? = null
//     private val TAG = "SupabaseBroadcastHelper"
// 
//     interface OnLocationReceivedListener {
//         fun onLocationReceived(latitude: Double, longitude: Double, heading: Float)
//     }
// 
//     fun joinChannel(missionId: String, onConnected: Runnable?) {
//         scope.launch {
//             try {
//                 val channelName = "tracking_mission_$missionId"
//                 Log.d(TAG, "Joining channel: $channelName")
//                 
//                 currentChannel = supabase.realtime.channel(channelName) {
//                     broadcast {
//                         ack = false
//                     }
//                 }
//                 currentChannel?.subscribe(blockUntilSubscribed = false)
//                 
//                 Log.d(TAG, "Successfully subscribed to channel: $channelName")
//                 if (onConnected != null) {
//                     withContext(Dispatchers.Main) {
//                         onConnected.run()
//                     }
//                 }
//             } catch (e: Exception) {
//                 Log.e(TAG, "Failed to join Supabase channel: ${e.message}", e)
//             }
//         }
//     }
// 
//     fun broadcastLocation(latitude: Double, longitude: Double, heading: Float) {
//         scope.launch {
//             try {
//                 currentChannel?.broadcast(
//                     event = "location_update",
//                     message = buildJsonObject {
//                         put("latitude", latitude)
//                         put("longitude", longitude)
//                         put("heading", heading)
//                     }
//                 )
//                 Log.d(TAG, "Broadcasted location: Lat=$latitude, Lng=$longitude, Heading=$heading")
//             } catch (e: Exception) {
//                 Log.e(TAG, "Failed to broadcast location: ${e.message}", e)
//             }
//         }
//     }
// 
//     fun listenForLocationUpdates(listener: OnLocationReceivedListener) {
//         scope.launch {
//             try {
//                 currentChannel?.onBroadcast("location_update") { payload: JsonObject ->
//                     val lat = payload["latitude"]?.jsonPrimitive?.doubleOrNull
//                     val lng = payload["longitude"]?.jsonPrimitive?.doubleOrNull
//                     val head = payload["heading"]?.jsonPrimitive?.floatOrNull
// 
//                     if (lat != null && lng != null) {
//                         scope.launch(Dispatchers.Main) {
//                             listener.onLocationReceived(lat, lng, head ?: 0f)
//                         }
//                     }
//                 }
//                 Log.d(TAG, "Listening for location updates")
//             } catch (e: Exception) {
//                 Log.e(TAG, "Failed to set up location listener: ${e.message}", e)
//             }
//         }
//     }
// 
//     fun disconnect() {
//         scope.launch {
//             try {
//                 Log.d(TAG, "Disconnecting from Supabase channel")
//                 currentChannel?.unsubscribe()
//                 currentChannel = null
//                 scope.cancel()
//             } catch (e: Exception) {
//                 Log.e(TAG, "Error during Supabase cleanup: ${e.message}", e)
//             }
//         }
//     }
// }
