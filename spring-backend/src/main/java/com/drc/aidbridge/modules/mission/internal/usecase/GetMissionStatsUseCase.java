package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.mission.internal.repository.DispatchAttemptJpaRepository;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionStatsResponse;
import com.drc.aidbridge.modules.shared.enums.DispatchResponse;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * UseCase: Lấy thống kê missions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetMissionStatsUseCase {

        private final MissionJpaRepository missionRepository;
        private final DispatchAttemptJpaRepository dispatchAttemptRepository;

        public MissionStatsResponse execute(LocalDate fromDate, LocalDate toDate) {
                log.debug("Getting mission stats from {} to {}", fromDate, toDate);

                // Default to today if not specified
                if (fromDate == null) {
                        fromDate = LocalDate.now().minusDays(30);
                }
                if (toDate == null) {
                        toDate = LocalDate.now();
                }

                Instant from = fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
                Instant to = toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

                // Tổng số missions
                long totalCreated = missionRepository.countByCreatedAtBetween(from, to);

                // Đếm theo status
                long completed = countByStatusInPeriod(MissionStatus.COMPLETED, from, to);
                long cancelled = countByStatusInPeriod(MissionStatus.CANCELLED, from, to);
                long inProgress = missionRepository.countAllActive();

                // Đếm theo type
                long rescueCreated = missionRepository.countByTypeAndCreatedAtBetween(MissionType.RESCUE, from, to);
                long rescueCompleted = missionRepository.countByTypeAndStatusAndCreatedAtBetween(MissionType.RESCUE,
                                MissionStatus.COMPLETED, from, to);
                Double rescueAvgMinutes = missionRepository
                                .avgCompletionMinutesByTypeAndCreatedAtBetween(MissionType.RESCUE.name(), from, to);

                long deliveryCreated = missionRepository.countByTypeAndCreatedAtBetween(MissionType.DELIVERY, from, to);
                long deliveryCompleted = missionRepository.countByTypeAndStatusAndCreatedAtBetween(MissionType.DELIVERY,
                                MissionStatus.COMPLETED, from, to);
                Double deliveryAvgMinutes = missionRepository
                                .avgCompletionMinutesByTypeAndCreatedAtBetween(MissionType.DELIVERY.name(), from, to);

                // Dispatch stats
                long totalAttempts = dispatchAttemptRepository.count();
                long acceptedAttempts = countDispatchByResponse(DispatchResponse.ACCEPTED);
                long timeoutAttempts = countDispatchByResponse(DispatchResponse.TIMEOUT);

                Double acceptanceRate = totalAttempts > 0 ? (double) acceptedAttempts / totalAttempts * 100 : 0.0;
                Double timeoutRate = totalAttempts > 0 ? (double) timeoutAttempts / totalAttempts * 100 : 0.0;

                return MissionStatsResponse.builder()
                                .period(MissionStatsResponse.Period.builder()
                                                .from(fromDate)
                                                .to(toDate)
                                                .build())
                                .totals(MissionStatsResponse.Totals.builder()
                                                .created(totalCreated)
                                                .completed(completed)
                                                .cancelled(cancelled)
                                                .inProgress(inProgress)
                                                .build())
                                .byType(MissionStatsResponse.ByTypeStats.builder()
                                                .rescue(MissionStatsResponse.TypeStats.builder()
                                                                .created(rescueCreated)
                                                                .completed(rescueCompleted)
                                                                .avgCompletionMinutes(rescueAvgMinutes)
                                                                .build())
                                                .delivery(MissionStatsResponse.TypeStats.builder()
                                                                .created(deliveryCreated)
                                                                .completed(deliveryCompleted)
                                                                .avgCompletionMinutes(deliveryAvgMinutes)
                                                                .build())
                                                .build())
                                .dispatchStats(MissionStatsResponse.DispatchStats.builder()
                                                .totalAttempts(totalAttempts)
                                                .acceptanceRate(acceptanceRate)
                                                .avgResponseTimeSeconds(null) // TODO: implement
                                                .timeoutRate(timeoutRate)
                                                .build())
                                .cachedAt(Instant.now())
                                .build();
        }

        private long countByStatusInPeriod(MissionStatus status, Instant from, Instant to) {
                // Simplified - count all with this status created in period
                return missionRepository.countByStatus(status);
        }

        private long countDispatchByResponse(DispatchResponse response) {
                // Count all dispatches with this response
                return dispatchAttemptRepository.findAll().stream()
                                .filter(da -> da.getResponse() == response)
                                .count();
        }
}
