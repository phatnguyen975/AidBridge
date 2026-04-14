package com.drc.aidbridge.modules.volunteer.internal.scheduler;

import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

// Scheduled task: Mark volunteers offline if heartbeat not received within timeout (every 60s)
@Slf4j
@Component
@RequiredArgsConstructor
public class VolunteerStatusScheduler {

    private static final long HEARTBEAT_TIMEOUT_MINUTES = 2;
    private final VolunteerJpaRepository volunteerRepository;

    // Runs every 60s: set isOnline=false if lastActiveAt < cutoff time (2 minutes ago)
    @Scheduled(fixedRate = 60000)
    public void markOfflineVolunteers() {
        try {
            // Calculate cutoff time: 2 minutes ago
            Instant cutoffTime = Instant.now()
                    .minus(HEARTBEAT_TIMEOUT_MINUTES, ChronoUnit.MINUTES);

            int updatedCount = volunteerRepository.updateOfflineVolunteers(cutoffTime);

            if (updatedCount > 0) {
                log.info("Marked {} volunteer(s) offline", updatedCount);
            } else {
                log.debug("No volunteers to mark offline");
            }
        } catch (Exception e) {
            log.error("Error in volunteer offline cleanup", e);
        }
    }
}
