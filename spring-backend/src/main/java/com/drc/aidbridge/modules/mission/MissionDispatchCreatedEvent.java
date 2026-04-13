package com.drc.aidbridge.modules.mission;

import com.drc.aidbridge.modules.shared.enums.DispatchType;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class MissionDispatchCreatedEvent {
    private final UUID missionId;
    private final MissionType missionType;
    private final DispatchType dispatchType;
    private final List<DispatchTarget> targets;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DispatchTarget {
        private final UUID volunteerId;
        private final UUID dispatchAttemptId;
        private final Integer batchNumber;
        private final Instant expiresAt;
    }
}
