package com.drc.aidbridge.modules.mission.internal.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho start mission (volunteer bắt đầu di chuyển).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartMissionRequest {

    /**
     * Vị trí hiện tại của volunteer khi start
     */
    private Double currentLat;
    private Double currentLng;
}
