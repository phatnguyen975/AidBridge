package com.drc.aidbridge.modules.shelter;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShelterDTO {
    private UUID id;
    private UUID hubId;
    private String name;
    private String address;
    private Integer maxCapacity;
    private Integer currentCapacity;
    private String phoneNumber;
    private String imageUrl;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private LocationDTO location;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDTO {
        private BigDecimal lat;
        private BigDecimal lng;
    }
}
