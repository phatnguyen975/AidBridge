package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.CreateMissionRequest;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateMissionUseCaseTest {

    @Mock
    private MissionJpaRepository missionRepository;

    @Mock
    private DispatchMissionUseCase dispatchMissionUseCase;

    @Captor
    private ArgumentCaptor<Mission> missionCaptor;

    @Captor
    private ArgumentCaptor<List<UUID>> preferredIdsCaptor;

    private CreateMissionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateMissionUseCase(missionRepository, dispatchMissionUseCase);
    }

    @Test
    void execute_shouldPersistPendingMissionAndDispatchWithNormalizedPreferredVolunteerIds() {
        UUID missionId = UUID.randomUUID();
        UUID volunteerA = UUID.randomUUID();
        UUID volunteerB = UUID.randomUUID();
        CreateMissionRequest request = CreateMissionRequest.builder()
                .missionType(MissionType.RESCUE)
                .volunteerId(volunteerA)
                .volunteerIds(List.of(volunteerA, volunteerB))
                .victimLat(BigDecimal.valueOf(10.7769))
                .victimLng(BigDecimal.valueOf(106.7009))
                .priorityScore(BigDecimal.valueOf(95))
                .comment("dispatch test")
                .build();

        when(missionRepository.save(any(Mission.class))).thenAnswer(invocation -> {
            Mission mission = invocation.getArgument(0);
            mission.setId(missionId);
            return mission;
        });

        MissionResponse expectedResponse = MissionResponse.builder()
                .id(missionId)
                .status(MissionStatus.DISPATCHING)
                .missionType(MissionType.RESCUE)
                .build();
        when(dispatchMissionUseCase.execute(eq(missionId), any())).thenReturn(expectedResponse);

        MissionResponse actualResponse = useCase.execute(request);

        assertSame(expectedResponse, actualResponse);
        verify(missionRepository).save(missionCaptor.capture());
        Mission savedMission = missionCaptor.getValue();
        assertEquals(MissionStatus.PENDING, savedMission.getStatus());
        assertEquals(MissionType.RESCUE, savedMission.getMissionType());
        assertEquals(BigDecimal.valueOf(10.7769), savedMission.getVictimLat());
        assertEquals(BigDecimal.valueOf(106.7009), savedMission.getVictimLng());

        verify(dispatchMissionUseCase).execute(eq(missionId), preferredIdsCaptor.capture());
        assertEquals(List.of(volunteerA, volunteerB), preferredIdsCaptor.getValue());
    }
}
