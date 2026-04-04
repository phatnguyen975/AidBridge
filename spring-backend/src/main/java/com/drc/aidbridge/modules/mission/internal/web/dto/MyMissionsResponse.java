package com.drc.aidbridge.modules.mission.internal.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO cho "My Missions" endpoint (volunteer xem missions của mình).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyMissionsResponse {

    /**
     * Các mission đang active (ASSIGNED, PICKING_UP, PICKED_UP, IN_TRANSIT)
     */
    private List<MissionResponse> active;

    /**
     * Các dispatch request đang chờ phản hồi (PENDING)
     */
    private List<PendingDispatch> pending;

    /**
     * Lịch sử missions (COMPLETED, CANCELLED)
     */
    private HistorySection history;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingDispatch {
        private DispatchAttemptResponse dispatchAttempt;
        private MissionResponse mission;
        private Instant expiresAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistorySection {
        private List<MissionResponse> items;
        private PaginationInfo pagination;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {
        private int page;
        private int limit;
        private long total;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }
}
