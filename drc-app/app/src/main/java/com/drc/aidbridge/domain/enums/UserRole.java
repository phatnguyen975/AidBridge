package com.drc.aidbridge.domain.enums;

/**
 * UserRole — enum representing the different roles a user can have in AidBridge.
 *
 * Roles determine which UI flows and screens the user has access to after login.
 * - GUEST:     Not logged in; can view map and send quick SOS.
 * - VICTIM:    Logged in as a person in need of aid.
 * - VOLUNTEER: Logged in as a disaster relief volunteer.
 * - SPONSOR:   Logged in as a donor / resource sponsor (Mạnh thường quân).
 * - STAFF:     Internal DRC staff managing hubs and inventory.
 * - ADMIN:     System administrator with full access.
 *
 * This enum is stored as a String in SharedPreferences via TokenManager.getUserRole()
 * and serialized/deserialized using name() / valueOf().
 */
public enum UserRole {
    GUEST,
    VICTIM,
    VOLUNTEER,
    SPONSOR,
    STAFF,
    ADMIN;

    /** Converts a string to a UserRole, defaulting to VICTIM if the string is invalid. */
    public static UserRole fromStringSafe(String roleStr) {
        if (roleStr == null || roleStr.trim().isEmpty()) {
            return VICTIM;
        }

        try {
            return UserRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return VICTIM;
        }
    }
}
