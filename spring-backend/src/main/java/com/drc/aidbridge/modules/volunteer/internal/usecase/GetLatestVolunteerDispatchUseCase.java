package com.drc.aidbridge.modules.volunteer.internal.usecase;

import com.drc.aidbridge.modules.mission.DispatchAttemptDTO;
import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.volunteer.internal.entity.Volunteer;
import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetLatestVolunteerDispatchUseCase {

    private final VolunteerJpaRepository volunteerRepository;
    private final MissionFacade missionFacade;

    @Transactional(readOnly = true)
    public DispatchAttemptDTO execute(UUID userId) {
        Volunteer volunteer = volunteerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found"));

        log.info("Fetching latest dispatch for userId: {}, mapped to volunteerId: {}", userId, volunteer.getId());

        return missionFacade.getLatestDispatchAttempt(userId).orElse(null);
    }
}
