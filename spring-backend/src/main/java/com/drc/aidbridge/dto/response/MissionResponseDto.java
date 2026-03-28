package com.drc.aidbridge.dto.response;

import com.drc.aidbridge.entity.enums.MissionStatus;
import com.drc.aidbridge.entity.enums.MissionType;
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
public class MissionResponseDto {

    private UUID id;
    private MissionType missionType;
    private MissionStatus status;

    // Related IDs
    private UUID sosRequestId;
    private UUID aidRequestId;
    private UUID helpRequestId;
    private UUID volunteerId;
    private UUID hubId;

    // Location
    private BigDecimal victimLat;
    private BigDecimal victimLng;

    // Mission details
    private String qrCodeToken;
    private BigDecimal priorityScore;
    private String cancellationReason;
    private String confirmationImageUrl;
    private String imageUrl;
    private String comment;

    // Timestamps
    private Instant acceptedAt;
    private Instant pickedUpAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private Instant createdAt;
    private Instant updatedAt;

    // Nested objects (populated when needed)
    private VolunteerBriefDto volunteer;
    private HubBriefDto hub;
    private SosRequestResponseDto sosRequest;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolunteerBriefDto {
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
    public static class HubBriefDto {
        private UUID id;
        private String name;
        private String address;
        private Double lat;
        private Double lng;
    }
}
