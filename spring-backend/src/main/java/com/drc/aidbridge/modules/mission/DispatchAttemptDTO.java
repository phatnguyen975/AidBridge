package com.drc.aidbridge.modules.mission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchAttemptDTO {
    private UUID id;
    private UUID missionId;
    private UUID volunteerId;
    private String dispatchType;
    private Integer batchNumber;
    private BigDecimal radiusKm;
    private String response;
}
