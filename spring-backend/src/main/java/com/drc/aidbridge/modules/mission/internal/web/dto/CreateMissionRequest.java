package com.drc.aidbridge.modules.mission.internal.web.dto;

import com.drc.aidbridge.modules.shared.enums.MissionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO cho tạo mission mới (Staff/Admin).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMissionRequest {

    /**
     * Loại mission: RESCUE hoặc DELIVERY
     */
    @NotNull(message = "Mission type is required")
    private MissionType missionType;

    /**
     * ID của SOS request (bắt buộc nếu RESCUE)
     */
    private UUID sosRequestId;

    /**
     * ID của Aid request (bắt buộc nếu DELIVERY)
     */
    private UUID aidRequestId;

    /**
     * ID của Hub (bắt buộc nếu DELIVERY)
     */
    private UUID hubId;

    /**
     * ID của volunteer (optional - nếu muốn pre-assign)
     */
    private UUID volunteerId;

    /**
     * Danh sách volunteer IDs (optional - để broadcast dispatch)
     */
    private List<UUID> volunteerIds;

    /**
     * Điểm ưu tiên (0-100)
     */
    private BigDecimal priorityScore;

    /**
     * Vị trí victim - latitude
     */
    private BigDecimal victimLat;

    /**
     * Vị trí victim - longitude
     */
    private BigDecimal victimLng;

    /**
     * Ghi chú
     */
    private String comment;
}
