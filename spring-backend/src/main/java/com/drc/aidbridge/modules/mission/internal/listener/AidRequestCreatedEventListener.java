package com.drc.aidbridge.modules.mission.internal.listener;

import com.drc.aidbridge.modules.aid.AidRequestCreatedEvent;
import com.drc.aidbridge.modules.mission.MissionFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AidRequestCreatedEventListener {

    private final MissionFacade missionFacade;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAidRequestCreated(AidRequestCreatedEvent event) {
        System.out.println("🔥 DELIVERY LISTENER TRIGGERED");

        if (event.getLat() == null || event.getLng() == null) {
            throw new IllegalStateException("Invalid AidRequest event: missing coordinates");
        }

        missionFacade.createDeliveryMission(
                event.getAidRequestId(),
                event.getLat(),
                event.getLng()
        );

        System.out.println("✅ DELIVERY MISSION SAVED");
    }
}
