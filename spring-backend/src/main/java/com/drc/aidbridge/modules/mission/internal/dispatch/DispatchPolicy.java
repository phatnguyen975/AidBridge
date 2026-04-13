package com.drc.aidbridge.modules.mission.internal.dispatch;

import java.time.Duration;
import java.util.List;

public final class DispatchPolicy {

    public static final Duration RESPONSE_TIMEOUT = Duration.ofMinutes(1);
    public static final List<Double> SEARCH_RADII_METERS = List.of(1000d, 3000d, 5000d, 10000d);
    public static final int SOS_BROADCAST_LIMIT = 5;
    public static final int AID_BATCH_SIZE = 1;

    private DispatchPolicy() {
    }
}
