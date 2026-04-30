package com.drc.aidbridge.sms;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SosSmsParser {

    @Inject
    public SosSmsParser() {
    }

    @Nullable
    public GatewaySmsSosPayload parse(@Nullable String senderPhone,
                                      @Nullable String rawMessage,
                                      long receivedAtGatewayMillis) {
        if (rawMessage == null) {
            return null;
        }

        String trimmed = rawMessage.trim();
        if (!trimmed.startsWith(SosSmsFormatter.PREFIX + "|")) {
            return null;
        }

        Map<String, String> values = parseValues(trimmed);
        String clientRequestId = safe(values.get("id"));
        if (clientRequestId.isEmpty()) {
            return null;
        }

        Double latitude = parseDouble(values.get("lat"));
        Double longitude = parseDouble(values.get("lng"));
        if (latitude == null || longitude == null || !isValidLatLng(latitude, longitude)) {
            return null;
        }

        Double accuracy = parseDouble(values.get("acc"));
        long triggeredAtMillis = parseLong(values.get("t"), receivedAtGatewayMillis);
        long locationCapturedAtMillis = parseLong(values.get("lc"), triggeredAtMillis);
        int peopleCount = Math.max(1, (int) parseLong(values.get("n"), 1L));
        boolean quickSos = !"0".equals(safe(values.get("q")));
        String phone = firstNonBlank(values.get("p"), senderPhone);

        return new GatewaySmsSosPayload(
            clientRequestId,
            phone,
            latitude,
            longitude,
            accuracy,
            triggeredAtMillis,
            locationCapturedAtMillis,
            peopleCount,
            quickSos,
            trimmed,
            receivedAtGatewayMillis
        );
    }

    private Map<String, String> parseValues(String message) {
        Map<String, String> values = new HashMap<>();
        String[] parts = message.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            int separator = part.indexOf('=');
            if (separator <= 0 || separator == part.length() - 1) {
                continue;
            }
            values.put(part.substring(0, separator), part.substring(separator + 1));
        }
        return values;
    }

    @Nullable
    private Double parseDouble(@Nullable String value) {
        String safeValue = safe(value);
        if (safeValue.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(safeValue);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private long parseLong(@Nullable String value, long fallback) {
        String safeValue = safe(value);
        if (safeValue.isEmpty()) {
            return fallback;
        }
        try {
            return Long.parseLong(safeValue);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private boolean isValidLatLng(double latitude, double longitude) {
        return latitude >= -90d && latitude <= 90d && longitude >= -180d && longitude <= 180d;
    }

    private String firstNonBlank(@Nullable String first, @Nullable String second) {
        String safeFirst = safe(first);
        if (!safeFirst.isEmpty()) {
            return safeFirst;
        }
        return safe(second);
    }

    private String safe(@Nullable String value) {
        return value != null ? value.trim() : "";
    }
}
