package com.drc.aidbridge.modules.mission.internal.scheduler;

import com.drc.aidbridge.modules.mission.internal.entity.DispatchAttempt;
import com.drc.aidbridge.modules.mission.internal.repository.DispatchAttemptJpaRepository;
import com.drc.aidbridge.modules.shared.enums.DispatchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DispatchTimeoutScheduler {

    private final DispatchAttemptJpaRepository dispatchAttemptRepository;

    /**
     * Cron job chạy mỗi 1 phút để cập nhật các dispatch attempt bị timeout (quá 5 phút).
     */
    @Scheduled(cron = "0 */1 * * * *")
    @Transactional
    public void checkDispatchTimeouts() {
        log.debug("Running cron job to check dispatch timeouts...");
        
        Instant expiryTime = Instant.now().minus(2, ChronoUnit.MINUTES);
        List<DispatchAttempt> expiredAttempts = dispatchAttemptRepository.findExpiredAttempts(
                DispatchResponse.PENDING, 
                expiryTime
        );

        if (expiredAttempts.isEmpty()) {
            return;
        }

        log.info("Found {} expired dispatch attempts. Updating to TIMEOUT.", expiredAttempts.size());
        
        for (DispatchAttempt attempt : expiredAttempts) {
            attempt.setResponse(DispatchResponse.TIMEOUT);
            attempt.setRespondedAt(Instant.now());
        }
        
        dispatchAttemptRepository.saveAll(expiredAttempts);
    }
}
