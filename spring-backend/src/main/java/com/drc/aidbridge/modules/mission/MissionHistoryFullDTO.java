package com.drc.aidbridge.modules.mission;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import com.drc.aidbridge.modules.sos.SosDTO;
import com.drc.aidbridge.modules.aid.AidRequestDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissionHistoryFullDTO {
    private UUID id;
    private UUID sosRequestId;
    private UUID aidRequestId;
    private UUID volunteerId;
    private UUID hubId;
    private String missionType;
    private MissionStatus status;
    private String qrCodeToken;
    private BigDecimal priorityScore;
    private Double victimLat;
    private Double victimLng;
    private Instant acceptedAt;
    private Instant pickedUpAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private String cancellationReason;
    private String address;
    private String confirmationImageUrl;
    private String imageUrl;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;
    private Double radiusKm;
    private String description;

    private SosDTO sosRequestDetail;
    private AidRequestDTO aidRequestDetail;
}
