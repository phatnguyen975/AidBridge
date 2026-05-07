package com.drc.aidbridge.ui.map.realtime

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.broadcastFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonPrimitive
import android.util.Log
import io.ktor.client.engine.okhttp.OkHttp

/**
 * A Kotlin wrapper that facilitates accessing Supabase Realtime Broadcast functionality
 * from Java code without needing knowledge of Kotlin Coroutines or Flow.
 */
class SupabaseBroadcastHelper(
    private val supabaseUrl: String,
    private val anonKey: String
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val supabase = createSupabaseClient(supabaseUrl, anonKey) {
        httpEngine = OkHttp.create()
        install(Realtime)
    }
    private var currentChannel: RealtimeChannel? = null
    private val TAG = "SupabaseBroadcastHelper"

    interface OnLocationReceivedListener {
        fun onLocationReceived(latitude: Double, longitude: Double, heading: Float)
    }

    interface OnRouteReceivedListener {
        fun onRouteReceived(polyline: String)
    }

    fun joinChannel(missionId: String, onConnected: Runnable?) {
        scope.launch {
            try {
                val channelName = "tracking_mission_$missionId"
                Log.d(TAG, "Joining channel: $channelName")
                
                currentChannel = supabase.realtime.channel(channelName)
                currentChannel?.subscribe(blockUntilSubscribed = true)
                
                Log.d(TAG, "Successfully subscribed to channel: $channelName")
                if (onConnected != null) {
                    withContext(Dispatchers.Main) {
                        onConnected.run()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to join Supabase channel: ${e.message}", e)
            }
        }
    }

    fun broadcastLocation(latitude: Double, longitude: Double, heading: Float) {
        scope.launch {
            try {
                currentChannel?.broadcast(
                    event = "location_update",
                    message = buildJsonObject {
                        put("latitude", latitude)
                        put("longitude", longitude)
                        put("heading", heading)
                    }
                )
                Log.v(TAG, "Broadcasted location: Lat=$latitude, Lng=$longitude")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to broadcast location: ${e.message}", e)
            }
        }
    }

    fun broadcastRoute(polyline: String) {
        scope.launch {
            try {
                currentChannel?.broadcast(
                    event = "route_update",
                    message = buildJsonObject {
                        put("polyline", polyline)
                    }
                )
                Log.d(TAG, "Broadcasted encoded route")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to broadcast route: ${e.message}", e)
            }
        }
    }

    fun listenForLocationUpdates(listener: OnLocationReceivedListener) {
        scope.launch {
            try {
                currentChannel?.broadcastFlow<JsonObject>("location_update")?.collect { payload ->
                    val lat = payload["latitude"]?.jsonPrimitive?.doubleOrNull
                    val lng = payload["longitude"]?.jsonPrimitive?.doubleOrNull
                    val head = payload["heading"]?.jsonPrimitive?.floatOrNull

                    if (lat != null && lng != null) {
                        withContext(Dispatchers.Main) {
                            listener.onLocationReceived(lat, lng, head ?: 0f)
                        }
                    }
                }
                Log.d(TAG, "Listening for location updates")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set up location listener: ${e.message}", e)
            }
        }
    }

    fun listenForRouteUpdates(listener: OnRouteReceivedListener) {
        scope.launch {
            try {
                currentChannel?.broadcastFlow<JsonObject>("route_update")?.collect { payload ->
                    val polyline = payload["polyline"]?.jsonPrimitive?.content
                    if (polyline != null) {
                        withContext(Dispatchers.Main) {
                            listener.onRouteReceived(polyline)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set up route listener: ${e.message}", e)
            }
        }
    }

    fun disconnect() {
        scope.launch {
            try {
                Log.d(TAG, "Disconnecting from Supabase channel")
                currentChannel?.unsubscribe()
                currentChannel = null
                // Only cancel children, not the whole scope, so it can be reused
                scope.coroutineContext.cancelChildren()
            } catch (e: Exception) {
                Log.e(TAG, "Error during Supabase cleanup: ${e.message}", e)
            }
        }
    }
}
