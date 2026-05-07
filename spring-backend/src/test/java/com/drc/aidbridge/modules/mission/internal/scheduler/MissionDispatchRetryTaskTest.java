package com.drc.aidbridge.modules.mission.internal.scheduler;

import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.usecase.DispatchMissionUseCase;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionDispatchRetryTaskTest {

    @Mock
    private MissionJpaRepository missionRepository;

    @Mock
    private DispatchMissionUseCase dispatchMissionUseCase;

    private MissionDispatchRetryTask retryTask;

    @BeforeEach
    void setUp() {
        retryTask = new MissionDispatchRetryTask(missionRepository, dispatchMissionUseCase);
    }

    @Test
    void retryPendingDispatches_shouldBulkFailMissionsThatWouldReachMaxRetries() {
        Mission exhaustedMission = retryMission(4, Instant.now().minusSeconds(120));
        Mission retryableMission = retryMission(3, Instant.now().minusSeconds(120));

        when(missionRepository.findMissionsForRetry(anyList(), any(Instant.class), eq(5)))
                .thenReturn(List.of(exhaustedMission, retryableMission));
        when(missionRepository.markDispatchesFailed(
                eq(List.of(exhaustedMission.getId())),
                eq(MissionStatus.DISPATCH_FAILED),
                eq(5),
                any(Instant.class)))
                .thenReturn(1);

        retryTask.retryPendingDispatches();

        verify(missionRepository).markDispatchesFailed(
                eq(List.of(exhaustedMission.getId())),
                eq(MissionStatus.DISPATCH_FAILED),
                eq(5),
                any(Instant.class));
        verify(dispatchMissionUseCase, never()).execute(exhaustedMission.getId(), null);
        verify(dispatchMissionUseCase).execute(retryableMission.getId(), null);
    }

    private Mission retryMission(int retryCount, Instant lastDispatchAt) {
        return Mission.builder()
                .id(UUID.randomUUID())
                .missionType(MissionType.RESCUE)
                .status(MissionStatus.DISPATCHING)
                .retryCount(retryCount)
                .lastDispatchAt(lastDispatchAt)
                .build();
    }
}
