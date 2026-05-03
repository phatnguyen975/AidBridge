package com.drc.aidbridge.modules.mission.internal.dispatch;

import java.time.Duration;


public final class DispatchPolicy {

    public static final Duration RESPONSE_TIMEOUT = Duration.ofMinutes(1);
    public static final int SOS_BROADCAST_LIMIT = 10;
    public static final int AID_BATCH_SIZE = 3;

    private DispatchPolicy() {
    }
}
