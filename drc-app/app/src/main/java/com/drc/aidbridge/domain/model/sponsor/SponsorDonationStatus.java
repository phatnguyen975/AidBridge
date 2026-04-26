package com.drc.aidbridge.domain.model.sponsor;

import androidx.annotation.Nullable;

public enum SponsorDonationStatus {
    REGISTERED,
    RECEIVED,
    OUTDATED;

    @Nullable
    public static SponsorDonationStatus fromApiValue(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        for (SponsorDonationStatus status : values()) {
            if (status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }

        return null;
    }
}
