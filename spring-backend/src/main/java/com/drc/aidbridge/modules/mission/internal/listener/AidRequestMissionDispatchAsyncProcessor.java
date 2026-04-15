package com.drc.aidbridge.modules.mission.internal.listener;

import com.drc.aidbridge.modules.aid.AidRequestCreatedEvent;
import com.drc.aidbridge.modules.mission.MissionDTO;
import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.mission.internal.usecase.DispatchMissionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AidRequestMissionDispatchAsyncProcessor {

    private final MissionFacade missionFacade;
    private final DispatchMissionUseCase dispatchMissionUseCase;

    @Async("missionTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(AidRequestCreatedEvent event) {
        if (event.getLat() == null || event.getLng() == null) {
            throw new IllegalStateException("Invalid AidRequest event: missing coordinates");
        }

        MissionDTO mission = missionFacade.createDeliveryMission(
                event.getAidRequestId(),
                event.getLat(),
                event.getLng());

        dispatchMissionUseCase.execute(mission.getId(), null);
        log.info("Created and dispatched mission {} for aid request {}", mission.getId(), event.getAidRequestId());
    }
}
