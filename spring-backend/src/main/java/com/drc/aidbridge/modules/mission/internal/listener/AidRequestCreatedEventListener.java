package com.drc.aidbridge.modules.mission.internal.listener;

import com.drc.aidbridge.modules.aid.AidRequestCreatedEvent;
import com.drc.aidbridge.modules.mission.MissionDTO;
import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.mission.internal.usecase.DispatchMissionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AidRequestCreatedEventListener {

    private final MissionFacade missionFacade;
    private final DispatchMissionUseCase dispatchMissionUseCase;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAidRequestCreated(AidRequestCreatedEvent event) {
        if (event.getLat() == null || event.getLng() == null) {
            throw new IllegalStateException("Invalid AidRequest event: missing coordinates");
        }

        MissionDTO mission = missionFacade.createDeliveryMission(
                event.getAidRequestId(),
                event.getLat(),
                event.getLng());

        dispatchMissionUseCase.execute(mission.getId(), null);
    }
}
