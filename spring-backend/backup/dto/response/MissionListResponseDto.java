package com.drc.aidbridge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionListResponseDto {

    private List<MissionResponseDto> items;
    private PaginationDto pagination;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationDto {
        private int page;
        private int limit;
        private long total;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }
}
