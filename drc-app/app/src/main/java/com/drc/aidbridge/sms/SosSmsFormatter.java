package com.drc.aidbridge.sms;

import androidx.annotation.Nullable;

import java.text.Normalizer;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SosSmsFormatter {

    public static final String PREFIX = "AIDBRIDGE_SOS";

    @Inject
    public SosSmsFormatter() {
    }

    public String formatQuickSos(String clientRequestId,
                                 double latitude,
                                 double longitude,
                                 @Nullable Double accuracy,
                                 long triggeredAtMillis,
                                 int peopleCount,
                                 boolean quickSos,
                                 @Nullable String phone,
                                 @Nullable String note) {
        StringBuilder builder = new StringBuilder();
        builder.append(PREFIX)
            .append("|id=").append(safe(clientRequestId))
            .append("|lat=").append(formatDouble(latitude, 6))
            .append("|lng=").append(formatDouble(longitude, 6));

        if (accuracy != null) {
            builder.append("|acc=").append(formatDouble(accuracy, 1));
        }

        builder.append("|t=").append(Math.max(0L, triggeredAtMillis))
            .append("|n=").append(Math.max(1, peopleCount))
            .append("|q=").append(quickSos ? "1" : "0");

        String normalizedPhone = safe(phone);
        if (!normalizedPhone.isEmpty()) {
            builder.append("|p=").append(normalizedPhone);
        }

        String normalizedNote = normalizeSmsText(note);
        if (!normalizedNote.isEmpty()) {
            builder.append("|note=").append(normalizedNote);
        }

        return builder.toString();
    }

    private String formatDouble(double value, int decimals) {
        return String.format(Locale.US, "%." + decimals + "f", value);
    }

    private String safe(@Nullable String value) {
        return value != null ? value.trim() : "";
    }

    private String normalizeSmsText(@Nullable String value) {
        String safeValue = safe(value);
        if (safeValue.isEmpty()) {
            return "";
        }

        String withoutMarks = Normalizer.normalize(safeValue, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        String compact = withoutMarks
            .replace("|", " ")
            .replace("=", " ")
            .replaceAll("\\s+", " ")
            .trim();
        return compact.length() > 80 ? compact.substring(0, 80) : compact;
    }
}
