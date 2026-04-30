package com.drc.aidbridge.modules.volunteer.internal.usecase;

import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.mission.MissionHistoryFullDTO;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetVolunteerCurrentMissionUseCase {

    private final VolunteerJpaRepository volunteerRepository;
    private final MissionFacade missionFacade;

    @Transactional(readOnly = true)
    public MissionHistoryFullDTO execute(UUID userId) {
        if (!volunteerRepository.existsByUserId(userId)) {
            throw new ResourceNotFoundException("Volunteer profile not found");
        }

        log.debug("Resolving current active mission for user {}", userId);
        return missionFacade.getCurrentMissionByVolunteerId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active mission found for volunteer"));
    }
}
