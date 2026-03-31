package com.drc.aidbridge.dto.response;

import com.drc.aidbridge.entity.enums.MissionStatus;
import com.drc.aidbridge.entity.enums.MissionType;
import com.drc.aidbridge.entity.enums.SosStatus;
import com.drc.aidbridge.entity.enums.UrgencyLevel;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SosRequestResponseDto {

    private UUID id;
    private UUID requesterId;
    private Double lat;
    private Double lng;
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
