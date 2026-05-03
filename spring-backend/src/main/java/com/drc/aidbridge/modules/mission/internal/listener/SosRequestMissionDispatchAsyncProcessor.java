package com.drc.aidbridge.modules.mission.internal.listener;

import com.drc.aidbridge.modules.mission.MissionDTO;
import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.mission.internal.usecase.DispatchMissionUseCase;
import com.drc.aidbridge.modules.sos.SosRequestCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SosRequestMissionDispatchAsyncProcessor {

    private final MissionFacade missionFacade;
    private final DispatchMissionUseCase dispatchMissionUseCase;

    @Async("missionTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(SosRequestCreatedEvent event) {
        if (event.getLat() == null || event.getLng() == null) {
            throw new IllegalStateException("Invalid SOS event: missing coordinates");
        }

        MissionDTO mission = missionFacade.createRescueMission(
                event.getSosRequestId(),
                event.getLat(),
                event.getLng());

        dispatchMissionUseCase.execute(mission.getId(), null);
        log.info("Created and dispatched SOS mission {} for sos request {}", mission.getId(), event.getSosRequestId());
    }
}
