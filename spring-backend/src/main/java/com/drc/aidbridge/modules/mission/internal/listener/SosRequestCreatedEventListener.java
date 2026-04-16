package com.drc.aidbridge.modules.mission.internal.listener;

import com.drc.aidbridge.modules.sos.SosRequestCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SosRequestCreatedEventListener {

    private final SosRequestMissionDispatchAsyncProcessor asyncProcessor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSosRequestCreated(SosRequestCreatedEvent event) {
        asyncProcessor.process(event);
    }
}
