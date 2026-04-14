package com.drc.aidbridge.modules.volunteer.internal.repository;

/**
 * Projection interface for volunteer statistics.
 * Spring Data will map native query column aliases to these getters.
 */
public interface VolunteerStatisticsProjection {
    Long getTotalTasksCompleted();
    Long getRescueMissions();
    Long getDeliveryMissions();
    Double getAvgRating();
    Long getTotalRatings();
    Double getAvgResponseSeconds();
    Long getPeopleHelped();
}
