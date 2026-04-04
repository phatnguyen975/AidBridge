package com.drc.aidbridge.modules.mission.internal.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO cho manual assign volunteer vào mission (Staff/Admin).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignMissionRequest {

    /**
     * ID của volunteer cần assign
     */
    @NotNull(message = "Volunteer ID is required")
    private UUID volunteerId;
}
