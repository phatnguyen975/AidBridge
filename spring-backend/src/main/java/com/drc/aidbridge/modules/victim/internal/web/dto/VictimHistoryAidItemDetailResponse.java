package com.drc.aidbridge.modules.victim.internal.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Item detail row for aid request history detail.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VictimHistoryAidItemDetailResponse {

    private String categoryName;
    private Integer quantity;
    private String unit;
}
