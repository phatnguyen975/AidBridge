package com.drc.aidbridge.ui.map.victim;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drc.aidbridge.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Minimal Google Geocoding API client used for searching and pinning one relative location.
 */
public class GoogleGeocodingClient {

    private static final String GEOCODING_ENDPOINT = "https://maps.googleapis.com/maps/api/geocode/json";
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 8000;

    // Vietnam geographic bounds for search restriction.
    private static final double VN_SOUTH_LAT = 8.17d;
    private static final double VN_WEST_LNG = 102.14d;
    private static final double VN_NORTH_LAT = 23.39d;
    private static final double VN_EAST_LNG = 109.47d;

    @Nullable
    public GeocodingResult geocodeFirstAddress(@NonNull Context context,
                                               @NonNull String query) throws IOException {
        List<GeocodingResult> suggestions = geocodeAddressSuggestions(context, query, 1);
        return suggestions.isEmpty() ? null : suggestions.get(0);
    }

    @NonNull
    public List<GeocodingResult> geocodeAddressSuggestions(@NonNull Context context,
                                                           @NonNull String query,
                                                           int maxResults) throws IOException {
        String trimmedQuery = query != null ? query.trim() : "";
        int safeMaxResults = Math.max(1, maxResults);
        if (trimmedQuery.isEmpty()) {
            return new ArrayList<>();
        }

        List<GeocodingResult> geocoderResults =
            searchWithAndroidGeocoder(context, trimmedQuery, safeMaxResults);
        if (!geocoderResults.isEmpty()) {
            return geocoderResults;
        }

        String apiKey = BuildConfig.MAPS_API_KEY != null ? BuildConfig.MAPS_API_KEY.trim() : "";
        if (apiKey.isEmpty()) {
            return new ArrayList<>();
        }

        // TODO(test-cost): Disable this call or switch to mock data after QA to avoid paid API usage.
        String url = GEOCODING_ENDPOINT
            + "?address=" + URLEncoder.encode(trimmedQuery, StandardCharsets.UTF_8)
            + "&language=vi"
            + "&region=vn"
            + "&components=" + URLEncoder.encode("country:VN", StandardCharsets.UTF_8)
            + "&bounds=" + VN_SOUTH_LAT + "," + VN_WEST_LNG + "|" + VN_NORTH_LAT + "," + VN_EAST_LNG
            + "&key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.connect();

            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

            if (stream == null) {
                return new ArrayList<>();
            }

            String body = readAsText(stream);
            return parseResults(body, safeMaxResults);
        } catch (JSONException exception) {
            String message = exception.getMessage();
            if (message == null || message.trim().isEmpty()) {
                message = "Invalid geocoding response format";
            }
            throw new IOException(message, exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Nullable
    private GeocodingResult parseFirstResult(String body) throws JSONException {
        List<GeocodingResult> results = parseResults(body, 1);
        return results.isEmpty() ? null : results.get(0);
    }

    @NonNull
    private List<GeocodingResult> parseResults(String body, int maxResults) throws JSONException {
        List<GeocodingResult> parsedResults = new ArrayList<>();
        if (body == null || body.trim().isEmpty()) {
            return parsedResults;
        }

        JSONObject root = new JSONObject(body);
        String status = root.optString("status", "");
        if (!"OK".equalsIgnoreCase(status)) {
            if ("ZERO_RESULTS".equalsIgnoreCase(status)) {
                return parsedResults;
            }

            String errorMessage = root.optString("error_message", "").trim();
            String composed = errorMessage.isEmpty()
                ? "Geocoding API error: " + status
                : "Geocoding API error: " + status + " - " + errorMessage;
            throw new JSONException(composed);
        }

        JSONArray results = root.optJSONArray("results");
        if (results == null || results.length() == 0) {
            return parsedResults;
        }

        for (int index = 0; index < results.length() && parsedResults.size() < maxResults; index++) {
            JSONObject candidate = results.optJSONObject(index);
            GeocodingResult parsed = parseCandidate(candidate);
            if (parsed == null) {
                continue;
            }

            if (isInVietnamBounds(parsed.latitude, parsed.longitude)) {
                parsedResults.add(parsed);
            }
        }

        return parsedResults;
    }

    @Nullable
    private GeocodingResult parseCandidate(@Nullable JSONObject candidate) {
        if (candidate == null) {
            return null;
        }

        JSONObject geometry = candidate.optJSONObject("geometry");
        JSONObject location = geometry != null ? geometry.optJSONObject("location") : null;
        if (location == null) {
            return null;
        }

        double latitude = location.optDouble("lat", Double.NaN);
        double longitude = location.optDouble("lng", Double.NaN);
        if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
            return null;
        }

        String formattedAddress = candidate.optString("formatted_address", "").trim();
        if (formattedAddress.isEmpty()) {
            formattedAddress = candidate.optString("place_id", "").trim();
        }

        return new GeocodingResult(latitude, longitude, formattedAddress);
    }

    @Nullable
    @NonNull
    private List<GeocodingResult> searchWithAndroidGeocoder(@NonNull Context context,
                                                            @NonNull String query,
                                                            int maxResults) {
        List<GeocodingResult> results = new ArrayList<>();
        if (!Geocoder.isPresent()) {
            return results;
        }

        try {
            Geocoder geocoder = new Geocoder(context.getApplicationContext(), new Locale("vi", "VN"));
            List<Address> addresses = geocoder.getFromLocationName(
                query,
                Math.max(1, maxResults),
                VN_SOUTH_LAT,
                VN_WEST_LNG,
                VN_NORTH_LAT,
                VN_EAST_LNG
            );

            if (addresses == null || addresses.isEmpty()) {
                return results;
            }

            for (Address address : addresses) {
                if (address == null) {
                    continue;
                }

                double lat = address.getLatitude();
                double lng = address.getLongitude();
                if (!isInVietnamBounds(lat, lng)) {
                    continue;
                }

                String line = address.getAddressLine(0);
                String formattedAddress = line != null ? line.trim() : "";
                results.add(new GeocodingResult(lat, lng, formattedAddress));
                if (results.size() >= maxResults) {
                    break;
                }
            }
        } catch (IOException ignored) {
            // Fall back to REST geocoding if local geocoder lookup fails.
        }

        return results;
    }

    private boolean isInVietnamBounds(double latitude, double longitude) {
        return latitude >= VN_SOUTH_LAT
            && latitude <= VN_NORTH_LAT
            && longitude >= VN_WEST_LNG
            && longitude <= VN_EAST_LNG;
    }

    private String readAsText(InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    public static final class GeocodingResult {
        public final double latitude;
        public final double longitude;
        public final String formattedAddress;

        GeocodingResult(double latitude, double longitude, String formattedAddress) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.formattedAddress = formattedAddress != null ? formattedAddress.trim() : "";
        }
    }
}
