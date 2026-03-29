package com.drc.aidbridge.modules.volunteer.internal.web.dto;

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
public class VolunteerProfileInfo {
    private UUID id;
    private UUID userId;
    private boolean isOnline;
    private BigDecimal currentLat;
    private BigDecimal currentLng;
    private String vehicleType;
    private Integer totalTasksCompleted;
    private BigDecimal avgRating;
    private Integer avgResponseSeconds;
    private Instant createdAt;
    private Instant updatedAt;
}
