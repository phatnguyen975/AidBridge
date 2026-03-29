package com.drc.aidbridge.modules.volunteer.internal;

import com.drc.aidbridge.modules.volunteer.VolunteerDTO;
import com.drc.aidbridge.modules.volunteer.VolunteerFacade;
import com.drc.aidbridge.modules.volunteer.internal.mapper.VolunteerMapper;
import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerJpaRepository;
import com.drc.aidbridge.modules.volunteer.internal.usecase.CreateVolunteerProfileUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VolunteerFacadeImpl implements VolunteerFacade {

    private final VolunteerJpaRepository volunteerRepository;
    private final VolunteerMapper volunteerMapper;
    private final CreateVolunteerProfileUseCase createVolunteerProfileUseCase;

    @Override
    public Optional<VolunteerDTO> getVolunteerByUserId(UUID userId) {
        return volunteerRepository.findByUserId(userId).map(volunteerMapper::toDTO);
    }

    @Override
    public VolunteerDTO createVolunteerProfile(UUID userId) {
        return createVolunteerProfileUseCase.execute(userId);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return volunteerRepository.existsByUserId(userId);
    }
}
