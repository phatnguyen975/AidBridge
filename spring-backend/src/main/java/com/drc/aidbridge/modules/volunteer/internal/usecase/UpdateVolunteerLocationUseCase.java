package com.drc.aidbridge.modules.volunteer.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.volunteer.internal.entity.Volunteer;
import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerJpaRepository;
import com.drc.aidbridge.modules.volunteer.internal.web.dto.UpdateVolunteerLocationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateVolunteerLocationUseCase {

    private final VolunteerJpaRepository volunteerRepository;

    @Transactional
    public void execute(UUID userId, UpdateVolunteerLocationRequest request) {
        Volunteer volunteer = volunteerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found"));

        volunteer.setCurrentLat(request.getCurrentLat());
        volunteer.setCurrentLng(request.getCurrentLng());

        volunteerRepository.save(volunteer);
    }
}
