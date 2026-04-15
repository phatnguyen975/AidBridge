package com.drc.aidbridge.domain.enums;

public enum HubStatus {
    ACTIVE,
    INACTIVE,
    EMERGENCY;

    public static HubStatus fromStringSafe(String rawStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            return INACTIVE;
        }

        try {
            return HubStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return INACTIVE;
        }
    }
}
