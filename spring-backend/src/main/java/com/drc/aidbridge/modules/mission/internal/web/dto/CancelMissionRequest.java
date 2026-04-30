package com.drc.aidbridge.modules.mission.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelMissionRequest {

    @NotNull(message = "Mission ID is required")
    private UUID missionId;

    @NotBlank(message = "Cancellation reason is required")
    private String cancellationReason;
}
