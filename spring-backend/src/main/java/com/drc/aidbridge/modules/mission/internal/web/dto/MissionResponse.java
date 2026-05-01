package com.drc.aidbridge.modules.mission.internal.web.dto;

import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MissionResponse {

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

    private String qrCodeToken;
    private BigDecimal priorityScore;
    private String cancellationReason;
    private String confirmationImageUrl;
    private String imageUrl;
    private String comment;

    private Instant acceptedAt;
    private Instant pickedUpAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private Instant createdAt;
    private Instant updatedAt;

    private VolunteerBrief volunteer;
    private HubBrief hub;
    private SosRequestBrief sosRequest;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolunteerBrief {
        private UUID id;
        private String fullName;
        private String phoneNumber;
        private String avatarUrl;
        private Double currentLat;
        private Double currentLng;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HubBrief {
        private UUID id;
        private String name;
        private String address;
        private Double lat;
        private Double lng;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SosRequestBrief {
        private UUID id;
        private UUID requesterId;
        private BigDecimal lat;
        private BigDecimal lng;
        private String address;
        private String description;
        private Integer peopleCount;
        private String urgencyLevel;
        private String status;
        private String imageUrl;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
