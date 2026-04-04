package com.drc.aidbridge.modules.mission.internal.listener;

import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.sos.SosRequestCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;


@Component
@RequiredArgsConstructor
public class SosRequestCreatedEventListener {

    private final MissionFacade missionFacade;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSosRequestCreated(SosRequestCreatedEvent event) {
        System.out.println("🔥 LISTENER TRIGGERED");

        if (event.getLat() == null || event.getLng() == null) {
            throw new IllegalStateException("Invalid SOS event: missing coordinates");
        }

        missionFacade.createRescueMission(
                event.getSosRequestId(),
                event.getLat(),
                event.getLng()
        );

        System.out.println("✅ MISSION SAVED");
    }
}
