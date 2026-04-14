package com.drc.aidbridge.modules.mission.internal.repository;

import java.time.Instant;

public interface MissionHistoryProjection {
    String getMissionType();

    Instant getCompletedAt();

    String getAddress();
}