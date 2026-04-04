package com.drc.aidbridge.modules.sos.internal.web.dto;

import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import com.drc.aidbridge.modules.shared.enums.SosStatus;
import com.drc.aidbridge.modules.shared.enums.UrgencyLevel;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SosRequestResponse {
    private UUID id;
    private UUID requesterId;
    private BigDecimal lat;
    private BigDecimal lng;
    private String address;
    private String description;
    private Integer peopleCount;
    private UrgencyLevel urgencyLevel;
    private SosStatus status;
    private String imageUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID missionId;
    private MissionType missionType;
    private MissionStatus missionStatus;
}
