package com.drc.aidbridge.modules.mission.internal.web.dto;

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
public class CompleteMissionRequest {
    
    @NotNull(message = "Mission ID is required")
    private UUID missionId;
    
    private String notes;
}
