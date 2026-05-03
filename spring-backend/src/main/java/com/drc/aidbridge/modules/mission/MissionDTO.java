package com.drc.aidbridge.modules.mission;

import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionDTO {

    private UUID id;
    private MissionType missionType;
    private MissionStatus status;
    private UUID sosRequestId;
    private UUID aidRequestId;
    private UUID volunteerId;
    private UUID hubId;
    private String codeName;
    private BigDecimal victimLat;
    private BigDecimal victimLng;
    private Instant createdAt;
    private Instant updatedAt;
}
