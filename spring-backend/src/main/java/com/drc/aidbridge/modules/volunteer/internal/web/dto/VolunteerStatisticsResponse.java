package com.drc.aidbridge.modules.volunteer.internal.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerStatisticsResponse {
    private long totalTasksCompleted;
    private long rescueMissions;
    private long deliveryMissions;
    private Double avgRating;
    private long totalRatings;
    private Double avgResponseSeconds;
    private long peopleHelped;
    private Instant cachedAt;
}
