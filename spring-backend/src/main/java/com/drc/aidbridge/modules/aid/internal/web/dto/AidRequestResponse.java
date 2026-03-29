package com.drc.aidbridge.modules.aid.internal.web.dto;

import com.drc.aidbridge.modules.shared.enums.AidStatus;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import com.drc.aidbridge.modules.shared.enums.UrgencyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AidRequestResponse {

    private UUID id;
    private UUID requesterId;
    private UUID sosRequestId;
    private AidStatus status;
    private BigDecimal lat;
    private BigDecimal lng;
    private String address;
    private String description;
    private Integer numberAdult;
    private Integer numberElderly;
    private Integer numberChildren;
    private UrgencyLevel urgencyLevel;
    private List<AidItemResponse> items;
    private UUID missionId;
    private MissionType missionType;
    private MissionStatus missionStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
