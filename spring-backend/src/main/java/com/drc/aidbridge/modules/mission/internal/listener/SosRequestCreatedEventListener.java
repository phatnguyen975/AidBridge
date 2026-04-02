package com.drc.aidbridge.modules.mission.internal.listener;

import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.sos.SosRequestCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class SosRequestCreatedEventListener {

    private final MissionFacade missionFacade;

    @EventListener
    public void handleSosRequestCreated(SosRequestCreatedEvent event) {
        missionFacade.createRescueMission(
                event.getSosRequestId(),
                BigDecimal.valueOf(event.getLat()),
                BigDecimal.valueOf(event.getLng())
        );
    }
}
