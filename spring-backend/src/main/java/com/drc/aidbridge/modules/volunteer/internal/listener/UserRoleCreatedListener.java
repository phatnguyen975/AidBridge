package com.drc.aidbridge.modules.volunteer.internal.listener;
import com.drc.aidbridge.modules.user.UserRoleCreatedEvent;
import com.drc.aidbridge.modules.volunteer.VolunteerFacade;
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
public class UserRoleCreatedListener {
    
    private final VolunteerFacade volunteerFacade;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUserRoleCreated(UserRoleCreatedEvent event) {
        log.info("[LISTENER] Received UserRoleCreatedEvent for userId: {}, role: {}", event.getUserId(), event.getRole());
        if ("VOLUNTEER".equalsIgnoreCase(event.getRole())) {
            // transform userId to UUID and create volunteer profile
            try {
                UUID userId = UUID.fromString(event.getUserId());
                log.info("[LISTENER] Creating volunteer profile for userId: {}", userId);
                volunteerFacade.createVolunteerProfile(userId);
                log.info("[LISTENER] ✅ Volunteer profile created for userId: {}", event.getUserId());
            } catch (Exception e) {
                log.error("[LISTENER] ❌ Failed to create volunteer profile", e);
                throw e;
            }
        } else {
            log.info("[LISTENER] Skipping - role is not VOLUNTEER: {}", event.getRole());
        }
    }
}
