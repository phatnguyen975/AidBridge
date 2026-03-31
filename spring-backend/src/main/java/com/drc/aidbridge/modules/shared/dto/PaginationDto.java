package com.drc.aidbridge.modules.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationDto {
    private int page;
    private int limit;
    private long total;
    private int totalPages;
    private boolean hasNext;
}
