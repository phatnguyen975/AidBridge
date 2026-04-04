package com.drc.aidbridge.modules.mission.internal.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response DTO cho Active Missions endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveMissionsResponse {

    private List<MissionResponse> items;
    private ActiveStats stats;
    private Instant cachedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveStats {
        private int totalActive;
        private ByTypeStats byType;
        private Map<String, Integer> byStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ByTypeStats {
        private int rescue;
        private int delivery;
    }
}
