package com.drc.aidbridge.modules.volunteer.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerJpaRepository;
import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerStatisticsProjection;
import com.drc.aidbridge.modules.volunteer.internal.web.dto.VolunteerStatisticsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetVolunteerStatisticsUseCase {

    private final VolunteerJpaRepository volunteerRepository;

    @Transactional(readOnly = true)
    public VolunteerStatisticsResponse execute(UUID volunteerId) {
        log.debug("Querying statistics for volunteer: {}", volunteerId);

        // Verify volunteer exists
        if (!volunteerRepository.existsById(volunteerId)) {
            throw new ResourceNotFoundException("Volunteer not found with id: " + volunteerId);
        }

        VolunteerStatisticsProjection stats = volunteerRepository.findStatisticsByVolunteerId(volunteerId);
        if (stats == null) {
            throw new ResourceNotFoundException("Statistics not found for volunteer: " + volunteerId);
        }

        long totalTasksCompleted = stats.getTotalTasksCompleted() != null ? stats.getTotalTasksCompleted() : 0L;
        long rescueMissions = stats.getRescueMissions() != null ? stats.getRescueMissions() : 0L;
        long deliveryMissions = stats.getDeliveryMissions() != null ? stats.getDeliveryMissions() : 0L;
        Double avgRating = stats.getAvgRating();
        long totalRatings = stats.getTotalRatings() != null ? stats.getTotalRatings() : 0L;
        Double avgResponseSeconds = stats.getAvgResponseSeconds();
        long peopleHelped = stats.getPeopleHelped() != null ? stats.getPeopleHelped() : 0L;

        return VolunteerStatisticsResponse.builder()
                .totalTasksCompleted(totalTasksCompleted)
                .rescueMissions(rescueMissions)
                .deliveryMissions(deliveryMissions)
                .avgRating(avgRating)
                .totalRatings(totalRatings)
                .avgResponseSeconds(avgResponseSeconds)
                .peopleHelped(peopleHelped)
                .cachedAt(Instant.now())
                .build();
    }

}
