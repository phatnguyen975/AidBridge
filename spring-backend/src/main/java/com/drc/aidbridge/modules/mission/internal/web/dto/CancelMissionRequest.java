package com.drc.aidbridge.modules.mission.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelMissionRequest {

    @NotBlank(message = "Cancellation reason is required")
    private String cancellationReason;
}
