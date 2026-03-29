package com.drc.aidbridge.modules.mission.internal.web.dto;

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
public class MissionTrackingResponse {

    private UUID missionId;
    private MissionStatus status;
    private VolunteerLocation volunteer;
    private Destination destination;
    private String destinationAddress;
    private Integer etaMinutes;
    private Double distanceKm;
    private String websocketChannel;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolunteerLocation {
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
    public static class Destination {
        private Double lat;
        private Double lng;
    }
}
