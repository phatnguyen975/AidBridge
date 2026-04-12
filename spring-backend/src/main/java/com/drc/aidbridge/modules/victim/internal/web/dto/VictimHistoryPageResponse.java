package com.drc.aidbridge.modules.victim.internal.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated victim history payload compatible with Android paging parser.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VictimHistoryPageResponse {

    private List<VictimHistoryItemResponse> items;
    private int page;
    private int size;

    @JsonProperty("total_pages")
    private int totalPages;

    @JsonProperty("total_items")
    private long totalItems;

    @JsonProperty("has_next")
    private boolean hasNext;
}
