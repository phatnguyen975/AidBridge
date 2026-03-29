package com.drc.aidbridge.dto.response;

import com.drc.aidbridge.entity.enums.AidStatus;
import com.drc.aidbridge.entity.enums.MissionStatus;
import com.drc.aidbridge.entity.enums.MissionType;
import com.drc.aidbridge.entity.enums.UrgencyLevel;

import jakarta.persistence.Column;
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
public class AidRequestResponseDto {

    private UUID id;
    private UUID requesterId;
    private UUID sosRequestId;
    private AidStatus status;
    @Column(precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(precision = 9, scale = 6)
    private BigDecimal lng;
    private String address;
    private String description;
    private Integer numberAdult;
    private Integer numberElderly;
    private Integer numberChildren;
    private UrgencyLevel urgencyLevel;
    private List<AidRequestItemResponseDto> items;
    private UUID missionId;
    private MissionType missionType;
    private MissionStatus missionStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
