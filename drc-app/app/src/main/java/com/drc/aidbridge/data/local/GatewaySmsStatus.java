package com.drc.aidbridge.data.local;

public final class GatewaySmsStatus {
    private GatewaySmsStatus() {
    }

    public static final String RECEIVED_SMS = "RECEIVED_SMS";
    public static final String PARSED = "PARSED";
    public static final String PENDING_FORWARD = "PENDING_FORWARD";
    public static final String FORWARDED = "FORWARDED";
    public static final String FORWARD_FAILED = "FORWARD_FAILED";
    public static final String INVALID = "INVALID";
}
