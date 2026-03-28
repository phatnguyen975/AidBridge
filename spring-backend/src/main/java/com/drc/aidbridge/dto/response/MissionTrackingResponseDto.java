package com.drc.aidbridge.dto.response;

import com.drc.aidbridge.entity.enums.MissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionTrackingResponseDto {

    private UUID missionId;
    private MissionStatus status;
    private VolunteerLocationDto volunteer;
    private DestinationDto destination;
    private String destinationAddress;
    private Integer etaMinutes;
    private Double distanceKm;
    private String websocketChannel;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolunteerLocationDto {
        private UUID id;
        private String fullName;
        private String avatarUrl;
        private Double currentLat;
        private Double currentLng;
        private Instant lastUpdated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DestinationDto {
        private Double lat;
        private Double lng;
    }
}
