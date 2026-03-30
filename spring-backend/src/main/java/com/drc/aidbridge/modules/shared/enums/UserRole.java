package com.drc.aidbridge.modules.shared.enums;

/**
 * User roles for role-based access control.
 *
 * Hierarchy (from most to least privileges):
 * ADMIN > STAFF > VOLUNTEER/SPONSOR > VICTIM
 */
public enum UserRole {
    /**
     * Victims requesting aid during disasters.
     */
    VICTIM,

    /**
     * Volunteers delivering aid to victims.
     */
    VOLUNTEER,

    /**
     * Sponsors donating goods and resources.
     */
    SPONSOR,

    /**
     * Staff managing hubs and coordinating operations.
     */
    STAFF,

    /**
     * System administrators with full access.
     */
    ADMIN
}
