package com.drc.aidbridge.modules.volunteer.internal.listener;
import com.drc.aidbridge.modules.user.UserRoleCreatedEvent;
import com.drc.aidbridge.modules.volunteer.VolunteerFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;
@Component
@RequiredArgsConstructor
@Slf4j
public class UserRoleCreatedListener {
    
    private final VolunteerFacade volunteerFacade;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRoleCreated(UserRoleCreatedEvent event) {
        log.info("Received UserRoleCreatedEvent for userId: {}, role: {}", event.getUserId(), event.getRole());
        if ("VOLUNTEER".equalsIgnoreCase(event.getRole())) {
            // transform userId to UUID and create volunteer profile
            UUID userId = UUID.fromString(event.getUserId());
            volunteerFacade.createVolunteerProfile(userId);
            log.info("Volunteer profile created for userId: {}", event.getUserId());
        }
    }
}
