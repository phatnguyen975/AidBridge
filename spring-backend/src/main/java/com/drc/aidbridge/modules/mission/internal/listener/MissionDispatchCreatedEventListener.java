package com.drc.aidbridge.modules.mission.internal.listener;

import com.drc.aidbridge.modules.mission.MissionDispatchCreatedEvent;
import com.drc.aidbridge.modules.notification.NotificationFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissionDispatchCreatedEventListener {

    private final NotificationFacade notificationFacade;
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMissionDispatchCreated(MissionDispatchCreatedEvent event) {
        for (MissionDispatchCreatedEvent.DispatchTarget target : event.getTargets()) {
            try {
                notificationFacade.notifyDispatchRequest(
                        target.getVolunteerId(),
                        event.getMissionId(),
                        target.getDispatchAttemptId(),
                        target.getExpiresAt(),
                        event.getMissionType(),
                        event.getDispatchType());
            } catch (Exception e) {
                log.error("Failed to send dispatch notification for mission {}", event.getMissionId(), e);
            }

            try {
                Object payload = Map.of(
                        "type", "DISPATCH_REQUEST",
                        "missionId", event.getMissionId().toString(),
                        "dispatchAttemptId", target.getDispatchAttemptId().toString(),
                        "dispatchType", event.getDispatchType().name(),
                        "batchNumber", String.valueOf(target.getBatchNumber()),
                        "expiresAt", target.getExpiresAt().toString());
                messagingTemplate.convertAndSend(
                        "/topic/dispatch/" + target.getVolunteerId(),
                        payload);
            } catch (Exception e) {
                log.error("Failed to publish realtime dispatch event for mission {}", event.getMissionId(), e);
            }
        }
    }
}
