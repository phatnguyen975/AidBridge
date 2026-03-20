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
    private String requesterName;
    private String requesterPhone;
    private String victimName;
    private String victimPhone;
    private Double victimLat;
    private Double victimLng;
    private String victimAddress;
    private String description;
    private Integer peopleCount;
    private Boolean isOnBehalf;
    private UrgencyLevel urgencyLevel;
    private String aiSummary;
    private SosStatus status;
    private String imageUrl;
    private Instant createdAt;
    private Instant updatedAt;

    private UUID missionId;
    private MissionType missionType;
    private MissionStatus missionStatus;
}
