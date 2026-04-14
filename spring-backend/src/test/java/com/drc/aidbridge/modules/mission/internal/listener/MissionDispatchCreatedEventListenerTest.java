package com.drc.aidbridge.modules.mission.internal.listener;

import com.drc.aidbridge.modules.mission.MissionDispatchCreatedEvent;
import com.drc.aidbridge.modules.mission.internal.dispatch.DispatchPolicy;
import com.drc.aidbridge.modules.notification.NotificationFacade;
import com.drc.aidbridge.modules.shared.enums.DispatchType;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MissionDispatchCreatedEventListenerTest {

    @Mock
    private NotificationFacade notificationFacade;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MissionDispatchCreatedEventListener listener;

    @Captor
    private ArgumentCaptor<String> destinationCaptor;

    @Captor
    private ArgumentCaptor<Object> payloadCaptor;

    @Test
    void handleMissionDispatchCreated_shouldNotifyAndPublishRealtimeForEachTarget() {
        UUID missionId = UUID.randomUUID();
        UUID volunteerA = UUID.randomUUID();
        UUID volunteerB = UUID.randomUUID();
        UUID attemptA = UUID.randomUUID();
        UUID attemptB = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(DispatchPolicy.RESPONSE_TIMEOUT);

        MissionDispatchCreatedEvent event = MissionDispatchCreatedEvent.builder()
                .missionId(missionId)
                .missionType(MissionType.RESCUE)
                .dispatchType(DispatchType.BROADCAST)
                .targets(List.of(
                        target(volunteerA, attemptA, expiresAt, 1),
                        target(volunteerB, attemptB, expiresAt, 1)))
                .build();

        listener.handleMissionDispatchCreated(event);

        verify(notificationFacade, times(2)).notifyDispatchRequest(
                any(UUID.class),
                eqMission(missionId),
                any(UUID.class),
                eqInstant(expiresAt),
                eqMissionType(MissionType.RESCUE),
                eqDispatchType(DispatchType.BROADCAST));
        verify(messagingTemplate, times(2)).convertAndSend(destinationCaptor.capture(), payloadCaptor.capture());

        assertEquals(List.of("/topic/dispatch/" + volunteerA, "/topic/dispatch/" + volunteerB),
                destinationCaptor.getAllValues());

        Map<?, ?> firstPayload = assertMap(payloadCaptor.getAllValues().get(0));
        assertEquals("DISPATCH_REQUEST", firstPayload.get("type"));
        assertEquals(missionId.toString(), firstPayload.get("missionId"));
        assertEquals(attemptA.toString(), firstPayload.get("dispatchAttemptId"));
        assertEquals(DispatchType.BROADCAST.name(), firstPayload.get("dispatchType"));
        assertEquals("1", firstPayload.get("batchNumber"));
        assertEquals(expiresAt.toString(), firstPayload.get("expiresAt"));
    }

    @Test
    void handleMissionDispatchCreated_shouldContinueRealtimePublishingWhenNotificationFails() {
        UUID missionId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        UUID dispatchAttemptId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(DispatchPolicy.RESPONSE_TIMEOUT);

        doThrow(new RuntimeException("FCM unavailable"))
                .when(notificationFacade)
                .notifyDispatchRequest(any(UUID.class), any(UUID.class), any(UUID.class), any(Instant.class), any(), any());

        listener.handleMissionDispatchCreated(MissionDispatchCreatedEvent.builder()
                .missionId(missionId)
                .missionType(MissionType.DELIVERY)
                .dispatchType(DispatchType.SEQUENTIAL)
                .targets(List.of(target(volunteerId, dispatchAttemptId, expiresAt, 1)))
                .build());

        verify(messagingTemplate).convertAndSend(destinationCaptor.capture(), payloadCaptor.capture());
        assertEquals("/topic/dispatch/" + volunteerId, destinationCaptor.getValue());
        Map<?, ?> payload = assertMap(payloadCaptor.getValue());
        assertEquals(dispatchAttemptId.toString(), payload.get("dispatchAttemptId"));
    }

    private MissionDispatchCreatedEvent.DispatchTarget target(UUID volunteerId, UUID dispatchAttemptId,
                                                              Instant expiresAt, int batchNumber) {
        return MissionDispatchCreatedEvent.DispatchTarget.builder()
                .volunteerId(volunteerId)
                .dispatchAttemptId(dispatchAttemptId)
                .expiresAt(expiresAt)
                .batchNumber(batchNumber)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> assertMap(Object payload) {
        assertInstanceOf(Map.class, payload);
        return (Map<?, ?>) payload;
    }

    private UUID eqMission(UUID missionId) {
        return org.mockito.ArgumentMatchers.eq(missionId);
    }

    private Instant eqInstant(Instant expiresAt) {
        return org.mockito.ArgumentMatchers.eq(expiresAt);
    }

    private MissionType eqMissionType(MissionType missionType) {
        return org.mockito.ArgumentMatchers.eq(missionType);
    }

    private DispatchType eqDispatchType(DispatchType dispatchType) {
        return org.mockito.ArgumentMatchers.eq(dispatchType);
    }
}
