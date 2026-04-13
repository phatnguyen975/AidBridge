package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.mission.MissionDispatchCreatedEvent;
import com.drc.aidbridge.modules.mission.internal.cache.MissionCacheRedisSchema;
import com.drc.aidbridge.modules.mission.internal.entity.DispatchAttempt;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.DispatchAttemptJpaRepository;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import com.drc.aidbridge.modules.shared.enums.DispatchResponse;
import com.drc.aidbridge.modules.shared.enums.DispatchType;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import com.drc.aidbridge.modules.sos.SosFacade;
import com.drc.aidbridge.modules.user.UserFacade;
import com.drc.aidbridge.modules.volunteer.VolunteerDTO;
import com.drc.aidbridge.modules.volunteer.VolunteerFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchMissionUseCaseTest {

    @Mock
    private MissionJpaRepository missionRepository;

    @Mock
    private DispatchAttemptJpaRepository dispatchAttemptRepository;

    @Mock
    private VolunteerFacade volunteerFacade;

    @Mock
    private UserFacade userFacade;

    @Mock
    private SosFacade sosFacade;

    @Mock
    private MissionCacheRedisSchema missionCache;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<DispatchAttempt> dispatchAttemptCaptor;

    @Captor
    private ArgumentCaptor<MissionDispatchCreatedEvent> eventCaptor;

    private DispatchMissionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DispatchMissionUseCase(
                missionRepository,
                dispatchAttemptRepository,
                volunteerFacade,
                userFacade,
                sosFacade,
                new MissionMapper(),
                missionCache,
                eventPublisher);

        lenient().when(missionRepository.save(any(Mission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(dispatchAttemptRepository.getNextBatchNumber(any(UUID.class))).thenReturn(1);
        lenient().when(dispatchAttemptRepository.save(any(DispatchAttempt.class))).thenAnswer(invocation -> {
            DispatchAttempt attempt = invocation.getArgument(0);
            if (attempt.getId() == null) {
                attempt.setId(UUID.randomUUID());
            }
            if (attempt.getCreatedAt() == null) {
                attempt.setCreatedAt(Instant.now());
            }
            return attempt;
        });
        lenient().when(sosFacade.getSosRequestById(any(UUID.class))).thenReturn(Optional.empty());
    }

    @Test
    void execute_shouldMoveRescueMissionToDispatchingAndCreateBroadcastAttempts() {
        Mission mission = pendingMission(MissionType.RESCUE);
        VolunteerDTO volunteerA = onlineVolunteer(BigDecimal.valueOf(10.7770), BigDecimal.valueOf(106.7010));
        VolunteerDTO volunteerB = onlineVolunteer(BigDecimal.valueOf(10.7775), BigDecimal.valueOf(106.7015));

        when(missionRepository.findById(mission.getId())).thenReturn(Optional.of(mission));
        when(volunteerFacade.findNearbyVolunteers(eq(mission.getVictimLat()), eq(mission.getVictimLng()), anyDouble()))
                .thenReturn(List.of(volunteerA, volunteerB), List.of(), List.of(), List.of());
        when(missionRepository.findActiveByVolunteerId(volunteerA.getUserId())).thenReturn(Optional.empty());
        when(missionRepository.findActiveByVolunteerId(volunteerB.getUserId())).thenReturn(Optional.empty());

        MissionResponse response = useCase.execute(mission.getId(), null);

        assertEquals(MissionStatus.DISPATCHING, mission.getStatus());
        assertEquals(MissionStatus.DISPATCHING, response.getStatus());
        verify(dispatchAttemptRepository, times(2)).save(dispatchAttemptCaptor.capture());

        List<DispatchAttempt> attempts = dispatchAttemptCaptor.getAllValues();
        assertEquals(Set.of(volunteerA.getUserId(), volunteerB.getUserId()),
                attempts.stream().map(DispatchAttempt::getVolunteerId).collect(java.util.stream.Collectors.toSet()));
        assertTrue(attempts.stream().allMatch(attempt -> attempt.getDispatchType() == DispatchType.BROADCAST));
        assertTrue(attempts.stream().allMatch(attempt -> attempt.getResponse() == DispatchResponse.PENDING));

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        MissionDispatchCreatedEvent event = eventCaptor.getValue();
        assertEquals(mission.getId(), event.getMissionId());
        assertEquals(MissionType.RESCUE, event.getMissionType());
        assertEquals(DispatchType.BROADCAST, event.getDispatchType());
        assertEquals(2, event.getTargets().size());

        verify(missionCache).cacheMission(any(MissionResponse.class));
    }

    @Test
    void execute_shouldMoveDeliveryMissionToDispatchingAndCreateSequentialAttempt() {
        Mission mission = pendingMission(MissionType.DELIVERY);
        VolunteerDTO volunteerA = onlineVolunteer(BigDecimal.valueOf(10.7770), BigDecimal.valueOf(106.7010));
        VolunteerDTO volunteerB = onlineVolunteer(BigDecimal.valueOf(10.7780), BigDecimal.valueOf(106.7020));

        when(missionRepository.findById(mission.getId())).thenReturn(Optional.of(mission));
        when(volunteerFacade.findNearbyVolunteers(eq(mission.getVictimLat()), eq(mission.getVictimLng()), anyDouble()))
                .thenReturn(List.of(volunteerA, volunteerB));
        when(missionRepository.findActiveByVolunteerId(volunteerA.getUserId())).thenReturn(Optional.empty());

        MissionResponse response = useCase.execute(mission.getId(), null);

        assertEquals(MissionStatus.DISPATCHING, response.getStatus());
        verify(dispatchAttemptRepository).save(dispatchAttemptCaptor.capture());

        DispatchAttempt attempt = dispatchAttemptCaptor.getValue();
        assertEquals(volunteerA.getUserId(), attempt.getVolunteerId());
        assertEquals(DispatchType.SEQUENTIAL, attempt.getDispatchType());
        assertEquals(DispatchResponse.PENDING, attempt.getResponse());

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(DispatchType.SEQUENTIAL, eventCaptor.getValue().getDispatchType());
        assertEquals(1, eventCaptor.getValue().getTargets().size());
    }

    @Test
    void execute_shouldLeaveMissionPendingWhenNoEligibleVolunteerExists() {
        Mission mission = pendingMission(MissionType.RESCUE);

        when(missionRepository.findById(mission.getId())).thenReturn(Optional.of(mission));
        when(volunteerFacade.findNearbyVolunteers(eq(mission.getVictimLat()), eq(mission.getVictimLng()), anyDouble()))
                .thenReturn(List.of());

        MissionResponse response = useCase.execute(mission.getId(), null);

        assertEquals(MissionStatus.PENDING, mission.getStatus());
        assertEquals(MissionStatus.PENDING, response.getStatus());
        verify(dispatchAttemptRepository, never()).save(any(DispatchAttempt.class));
        verify(eventPublisher, never()).publishEvent(any());
        verify(missionRepository, never()).save(any(Mission.class));
        verifyNoInteractions(missionCache);
    }

    @Test
    void execute_shouldSkipDispatchWhenMissionIsNotPending() {
        Mission mission = pendingMission(MissionType.RESCUE);
        mission.setStatus(MissionStatus.ASSIGNED);

        when(missionRepository.findById(mission.getId())).thenReturn(Optional.of(mission));

        MissionResponse response = useCase.execute(mission.getId(), null);

        assertEquals(MissionStatus.ASSIGNED, response.getStatus());
        verifyNoInteractions(volunteerFacade, dispatchAttemptRepository, missionCache, eventPublisher);
        verify(missionRepository, never()).save(any(Mission.class));
    }

    @Test
    void execute_shouldUsePreferredVolunteersAndBroadcastWhenMoreThanOneIsEligible() {
        Mission mission = pendingMission(MissionType.RESCUE);
        VolunteerDTO volunteerA = onlineVolunteer(BigDecimal.valueOf(10.7770), BigDecimal.valueOf(106.7010));
        VolunteerDTO volunteerB = onlineVolunteer(BigDecimal.valueOf(10.7778), BigDecimal.valueOf(106.7018));

        when(missionRepository.findById(mission.getId())).thenReturn(Optional.of(mission));
        when(volunteerFacade.getVolunteerByUserId(volunteerA.getUserId())).thenReturn(Optional.of(volunteerA));
        when(volunteerFacade.getVolunteerByUserId(volunteerB.getUserId())).thenReturn(Optional.of(volunteerB));
        when(missionRepository.findActiveByVolunteerId(volunteerA.getUserId())).thenReturn(Optional.empty());
        when(missionRepository.findActiveByVolunteerId(volunteerB.getUserId())).thenReturn(Optional.empty());

        MissionResponse response = useCase.execute(
                mission.getId(),
                List.of(volunteerA.getUserId(), volunteerA.getUserId(), volunteerB.getUserId()));

        assertEquals(MissionStatus.DISPATCHING, response.getStatus());
        verify(dispatchAttemptRepository, times(2)).save(dispatchAttemptCaptor.capture());
        assertEquals(Set.of(volunteerA.getUserId(), volunteerB.getUserId()),
                dispatchAttemptCaptor.getAllValues().stream()
                        .map(DispatchAttempt::getVolunteerId)
                        .collect(java.util.stream.Collectors.toSet()));
        verify(volunteerFacade, never()).findNearbyVolunteers(any(), any(), anyDouble());
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(DispatchType.BROADCAST, eventCaptor.getValue().getDispatchType());
    }

    @Test
    void execute_shouldUseSequentialDispatchForSinglePreferredVolunteer() {
        Mission mission = pendingMission(MissionType.RESCUE);
        VolunteerDTO volunteer = onlineVolunteer(BigDecimal.valueOf(10.7770), BigDecimal.valueOf(106.7010));

        when(missionRepository.findById(mission.getId())).thenReturn(Optional.of(mission));
        when(volunteerFacade.getVolunteerByUserId(volunteer.getUserId())).thenReturn(Optional.of(volunteer));
        when(missionRepository.findActiveByVolunteerId(volunteer.getUserId())).thenReturn(Optional.empty());

        MissionResponse response = useCase.execute(mission.getId(), List.of(volunteer.getUserId()));

        assertEquals(MissionStatus.DISPATCHING, response.getStatus());
        verify(dispatchAttemptRepository).save(dispatchAttemptCaptor.capture());
        assertEquals(DispatchType.SEQUENTIAL, dispatchAttemptCaptor.getValue().getDispatchType());
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(DispatchType.SEQUENTIAL, eventCaptor.getValue().getDispatchType());
    }

    @Test
    void execute_shouldLeaveMissionPendingWhenNearbyVolunteerAlreadyHasActiveMission() {
        Mission mission = pendingMission(MissionType.DELIVERY);
        VolunteerDTO busyVolunteer = onlineVolunteer(BigDecimal.valueOf(10.7770), BigDecimal.valueOf(106.7010));
        Mission activeMission = pendingMission(MissionType.RESCUE);
        activeMission.setStatus(MissionStatus.ASSIGNED);

        when(missionRepository.findById(mission.getId())).thenReturn(Optional.of(mission));
        when(volunteerFacade.findNearbyVolunteers(eq(mission.getVictimLat()), eq(mission.getVictimLng()), anyDouble()))
                .thenReturn(List.of(busyVolunteer));
        when(missionRepository.findActiveByVolunteerId(busyVolunteer.getUserId())).thenReturn(Optional.of(activeMission));

        MissionResponse response = useCase.execute(mission.getId(), null);

        assertEquals(MissionStatus.PENDING, response.getStatus());
        verify(dispatchAttemptRepository, never()).save(any(DispatchAttempt.class));
        verify(eventPublisher, never()).publishEvent(any());
        verifyNoInteractions(missionCache);
    }

    @Test
    void execute_shouldDispatchPreferredVolunteerEvenWhenOfflineFlagIsFalse() {
        Mission mission = pendingMission(MissionType.RESCUE);
        VolunteerDTO offlineVolunteer = VolunteerDTO.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .isOnline(false)
                .currentLocation(null)
                .build();

        when(missionRepository.findById(mission.getId())).thenReturn(Optional.of(mission));
        when(volunteerFacade.getVolunteerByUserId(offlineVolunteer.getUserId())).thenReturn(Optional.of(offlineVolunteer));
        when(missionRepository.findActiveByVolunteerId(offlineVolunteer.getUserId())).thenReturn(Optional.empty());

        MissionResponse response = useCase.execute(mission.getId(), List.of(offlineVolunteer.getUserId()));

        assertEquals(MissionStatus.DISPATCHING, response.getStatus());
        verify(dispatchAttemptRepository).save(dispatchAttemptCaptor.capture());
        assertEquals(offlineVolunteer.getUserId(), dispatchAttemptCaptor.getValue().getVolunteerId());
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(DispatchType.SEQUENTIAL, eventCaptor.getValue().getDispatchType());
    }

    private Mission pendingMission(MissionType missionType) {
        return Mission.builder()
                .id(UUID.randomUUID())
                .missionType(missionType)
                .status(MissionStatus.PENDING)
                .priorityScore(BigDecimal.valueOf(90))
                .victimLocation(Mission.createPoint(BigDecimal.valueOf(10.7769), BigDecimal.valueOf(106.7009)))
                .build();
    }

    private VolunteerDTO onlineVolunteer(BigDecimal lat, BigDecimal lng) {
        return VolunteerDTO.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .isOnline(true)
                .currentLocation(VolunteerDTO.LocationDTO.builder()
                        .lat(lat)
                        .lng(lng)
                        .build())
                .build();
    }
}
