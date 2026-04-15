package com.drc.aidbridge.modules.mission.internal.listener;

import com.drc.aidbridge.modules.aid.AidRequestCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AidRequestCreatedEventListener {

    private final AidRequestMissionDispatchAsyncProcessor asyncProcessor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAidRequestCreated(AidRequestCreatedEvent event) {
        asyncProcessor.process(event);
    }
}
