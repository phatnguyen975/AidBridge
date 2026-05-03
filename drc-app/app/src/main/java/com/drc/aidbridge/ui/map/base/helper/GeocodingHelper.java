package com.drc.aidbridge.ui.map.base.helper;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeocodingHelper {

    public interface GeocodingListener {
        void onAddressResolved(@Nullable String addressText, boolean forStart);
    }

    @Nullable
    private GeocodingListener listener;
    private ExecutorService geocodeExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void setListener(@Nullable GeocodingListener listener) {
        this.listener = listener;
    }

    public void init() {
        if (geocodeExecutor == null || geocodeExecutor.isShutdown()) {
            geocodeExecutor = Executors.newSingleThreadExecutor();
        }
    }

    public void destroy() {
        if (geocodeExecutor != null) {
            geocodeExecutor.shutdownNow();
            geocodeExecutor = null;
        }
    }

    public void reverseGeocodeAsync(@NonNull GeoPoint point, boolean forStart) {
        init();
        geocodeExecutor.execute(() -> {
            String addressText = requestAddressFromNominatim(point);

            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onAddressResolved(addressText, forStart);
                }
            });
        });
    }

    @NonNull
    public String toCompactAddress(@Nullable String addressText, @NonNull android.content.Context context, int loadingResId) {
        if (addressText == null || addressText.trim().isEmpty()) {
            return context.getString(loadingResId);
        }

        int separatorIndex = addressText.indexOf(':');
        if (separatorIndex >= 0 && separatorIndex < addressText.length() - 1) {
            return addressText.substring(separatorIndex + 1).trim();
        }
        return addressText.trim();
    }

    @Nullable
    protected String requestAddressFromNominatim(@NonNull GeoPoint point) {
        HttpURLConnection connection = null;
        try {
            String urlValue = String.format(
                    Locale.US,
                    "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%.7f&lon=%.7f&zoom=18&addressdetails=1",
                    point.getLatitude(),
                    point.getLongitude());

            URL url = new URL(urlValue);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "AidBridge-Android/1.0 (contact@aidbridge.local)");

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(builder.toString());
            JSONObject address = json.optJSONObject("address");
            if (address != null) {
                String road = firstNonEmpty(address.optString("road"), address.optString("neighbourhood"),
                        address.optString("suburb"));
                String district = firstNonEmpty(address.optString("city_district"), address.optString("city"),
                        address.optString("state"));
                if (road != null && district != null) {
                    return road + ", " + district;
                }
                if (road != null) {
                    return road;
                }
            }

            String displayName = json.optString("display_name");
            return displayName != null ? displayName : null;
        } catch (Exception exception) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Nullable
    private String firstNonEmpty(@Nullable String first, @Nullable String second, @Nullable String third) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }
        if (third != null && !third.trim().isEmpty()) {
            return third.trim();
        }
        return null;
    }
}
