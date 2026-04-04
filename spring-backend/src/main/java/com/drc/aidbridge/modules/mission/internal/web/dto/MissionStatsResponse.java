package com.drc.aidbridge.modules.mission.internal.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO cho Mission Statistics endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionStatsResponse {

    private Period period;
    private Totals totals;
    private ByTypeStats byType;
    private DispatchStats dispatchStats;
    private Instant cachedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Period {
        private LocalDate from;
        private LocalDate to;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Totals {
        private long created;
        private long completed;
        private long cancelled;
        private long inProgress;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ByTypeStats {
        private TypeStats rescue;
        private TypeStats delivery;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeStats {
        private long created;
        private long completed;
        private Double avgCompletionMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispatchStats {
        private long totalAttempts;
        private Double acceptanceRate;
        private Double avgResponseTimeSeconds;
        private Double timeoutRate;
    }
}
