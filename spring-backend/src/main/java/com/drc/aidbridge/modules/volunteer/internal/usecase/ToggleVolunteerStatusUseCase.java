package com.drc.aidbridge.modules.volunteer.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import com.drc.aidbridge.modules.volunteer.internal.entity.Volunteer;
import com.drc.aidbridge.modules.volunteer.internal.mapper.VolunteerMapper;
import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerJpaRepository;
import com.drc.aidbridge.modules.volunteer.internal.web.dto.ToggleVolunteerStatusRequest;
import com.drc.aidbridge.modules.volunteer.internal.web.dto.VolunteerProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

// Handle explicit online/offline toggle from user
@Component
@RequiredArgsConstructor
public class ToggleVolunteerStatusUseCase {

    private final VolunteerJpaRepository volunteerRepository;
    private final UserFacade userFacade;
    private final VolunteerMapper volunteerMapper;

    @Transactional
    public VolunteerProfileResponse execute(UUID userId, ToggleVolunteerStatusRequest request) {
        UserDTO user = userFacade.getUserById(userId);

        Volunteer volunteer = volunteerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found"));

        // Cập nhật trạng thái online
        volunteer.setOnline(request.isOnline());

        // Update last active time if turning online to start heartbeat counter
        if (request.isOnline()) {
            volunteer.setLastActiveAt(Instant.now());
        }

        volunteer = volunteerRepository.save(volunteer);

        return volunteerMapper.toResponse(volunteer, user);
    }
}
