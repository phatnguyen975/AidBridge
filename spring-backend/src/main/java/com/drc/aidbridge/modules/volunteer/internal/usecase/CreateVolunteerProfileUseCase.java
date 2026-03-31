package com.drc.aidbridge.modules.volunteer.internal.usecase;

import com.drc.aidbridge.modules.volunteer.VolunteerDTO;
import com.drc.aidbridge.modules.volunteer.internal.entity.Volunteer;
import com.drc.aidbridge.modules.volunteer.internal.mapper.VolunteerMapper;
import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateVolunteerProfileUseCase {

    private final VolunteerJpaRepository volunteerRepository;
    private final VolunteerMapper volunteerMapper;

    @Transactional
    public VolunteerDTO execute(UUID userId) {
        Volunteer volunteer = Volunteer.builder()
                .userId(userId)
                .isOnline(false)
                .totalTasksCompleted(0)
                .build();

        volunteer = volunteerRepository.save(volunteer);
        log.info("Created volunteer profile for user: {}", userId);

        return volunteerMapper.toDTO(volunteer);
    }
}
