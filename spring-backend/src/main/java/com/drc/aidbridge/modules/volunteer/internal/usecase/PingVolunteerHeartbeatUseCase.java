package com.drc.aidbridge.modules.volunteer.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import com.drc.aidbridge.modules.volunteer.internal.entity.Volunteer;
import com.drc.aidbridge.modules.volunteer.internal.mapper.VolunteerMapper;
import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerJpaRepository;
import com.drc.aidbridge.modules.volunteer.internal.web.dto.PingVolunteerHeartbeatRequest;
import com.drc.aidbridge.modules.volunteer.internal.web.dto.VolunteerProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Handle heartbeat ping: Update status, location, and last active timestamp
@Component
@RequiredArgsConstructor
public class PingVolunteerHeartbeatUseCase {

    private final VolunteerJpaRepository volunteerRepository;
    private final UserFacade userFacade;
    private final VolunteerMapper volunteerMapper;

    // Update volunteer as online with current location and timestamp
    @Transactional
    public VolunteerProfileResponse execute(UUID userId, PingVolunteerHeartbeatRequest request) {
        UserDTO user = userFacade.getUserById(userId);

        Volunteer volunteer = volunteerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found"));

        // Set online status and update last active timestamp
        volunteer.setOnline(true);
        volunteer.setLastActiveAt(Instant.now());

        // Update current location (note: JTS Coordinate requires (lng, lat) order with SRID 4326)
        if (request.getLat() != null && request.getLng() != null) {
            volunteer.setCurrentLocation(
                    Volunteer.createPoint(
                            BigDecimal.valueOf(request.getLat()),
                            BigDecimal.valueOf(request.getLng())
                    )
            );
        }

        volunteer = volunteerRepository.save(volunteer);

        return volunteerMapper.toResponse(volunteer, user);
    }
}
