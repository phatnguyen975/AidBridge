package com.drc.aidbridge.modules.mission.internal.scheduler;

import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.usecase.DispatchMissionUseCase;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Task ngầm tự động tìm kiếm lại volunteer cho các mission đang treo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionDispatchRetryTask {

    private final MissionJpaRepository missionRepository;
    private final DispatchMissionUseCase dispatchMissionUseCase;

    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY_SECONDS = 45;

    @Scheduled(fixedDelay = 60000) // Chạy mỗi phút 1 lần
    public void retryPendingDispatches() {
        log.debug("Starting mission dispatch retry task...");

        Instant threshold = Instant.now().minus(RETRY_DELAY_SECONDS, ChronoUnit.SECONDS);
        List<MissionStatus> targetStatuses = List.of(MissionStatus.PENDING, MissionStatus.DISPATCHING);

        List<Mission> missionsToRetry = missionRepository.findMissionsForRetry(
                targetStatuses,
                threshold,
                MAX_RETRIES
        );

        if (missionsToRetry.isEmpty()) {
            return;
        }

        log.info("Found {} missions for dispatch retry", missionsToRetry.size());

        for (Mission mission : missionsToRetry) {
            try {
                log.info("Retrying dispatch for mission {} (Current Retry: {})", 
                        mission.getId(), mission.getRetryCount());
                
                // Thực hiện lại logic điều phối
                dispatchMissionUseCase.execute(mission.getId(), null);
            } catch (Exception e) {
                log.error("Failed to retry dispatch for mission {}", mission.getId(), e);
            }
        }
    }
}
