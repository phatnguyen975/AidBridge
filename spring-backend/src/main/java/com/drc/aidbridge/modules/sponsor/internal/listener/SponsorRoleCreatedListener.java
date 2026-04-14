package com.drc.aidbridge.modules.sponsor.internal.listener;

import com.drc.aidbridge.modules.user.UserRoleCreatedEvent;
import com.drc.aidbridge.modules.sponsor.SponsorFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SponsorRoleCreatedListener {
    
    private final SponsorFacade sponsorFacade;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUserRoleCreated(UserRoleCreatedEvent event) {
        log.info("[LISTENER] Received UserRoleCreatedEvent for userId: {}, role: {}", event.getUserId(), event.getRole());
        if ("SPONSOR".equalsIgnoreCase(event.getRole())) {
            // transform userId to UUID and create sponsor profile
            try {
                UUID userId = UUID.fromString(event.getUserId());
                log.info("[LISTENER] Creating sponsor profile for userId: {}", userId);
                sponsorFacade.createSponsorProfile(userId);
                log.info("[LISTENER] ✅ Sponsor profile created for userId: {}", event.getUserId());
            } catch (Exception e) {
                log.error("[LISTENER] ❌ Failed to create sponsor profile", e);
                throw e;
            }
        } else {
            log.info("[LISTENER] Skipping - role is not SPONSOR: {}", event.getRole());
        }
    }
}