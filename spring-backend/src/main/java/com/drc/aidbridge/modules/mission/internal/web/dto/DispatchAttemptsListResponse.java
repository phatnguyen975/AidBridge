package com.drc.aidbridge.modules.mission.internal.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO cho danh sách dispatch attempts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchAttemptsListResponse {

    private List<DispatchAttemptResponse> items;
    private PaginationInfo pagination;

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
