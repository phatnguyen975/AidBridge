package com.drc.aidbridge.modules.mission.internal.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO cho accept mission (volunteer chấp nhận dispatch).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptMissionRequest {

    /**
     * ID của dispatch attempt đang được respond
     */
    private UUID dispatchAttemptId;

    /**
     * Vị trí hiện tại của volunteer khi accept
     */
    private Double currentLat;
    private Double currentLng;
}
