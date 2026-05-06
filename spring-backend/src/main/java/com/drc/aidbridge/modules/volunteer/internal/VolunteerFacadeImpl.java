package com.drc.aidbridge.modules.volunteer.internal;

import com.drc.aidbridge.modules.volunteer.VolunteerDTO;
import com.drc.aidbridge.modules.volunteer.VolunteerFacade;
import com.drc.aidbridge.modules.volunteer.internal.entity.Volunteer;
import com.drc.aidbridge.modules.volunteer.internal.mapper.VolunteerMapper;
import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerJpaRepository;
import com.drc.aidbridge.modules.volunteer.internal.usecase.CreateVolunteerProfileUseCase;
import com.drc.aidbridge.modules.volunteer.internal.usecase.FindNearbyVolunteersUseCase;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VolunteerFacadeImpl implements VolunteerFacade {

    private final VolunteerJpaRepository volunteerRepository;
    private final VolunteerMapper volunteerMapper;
    private final CreateVolunteerProfileUseCase createVolunteerProfileUseCase;
    private final FindNearbyVolunteersUseCase findNearbyVolunteersUseCase;

    @Override
    public Optional<VolunteerDTO> getVolunteerByUserId(UUID userId) {
        return volunteerRepository.findByUserId(userId).map(volunteerMapper::toDTO);
    }

    @Override
    public List<VolunteerDTO> findNearbyVolunteers(BigDecimal lat, BigDecimal lng) {
        return findNearbyVolunteersUseCase.execute(lat, lng);
    }

    @Override
    public List<VolunteerDTO> findNearbyVolunteers(BigDecimal lat, BigDecimal lng, int retryCount) {
        return findNearbyVolunteersUseCase.execute(lat, lng, retryCount);
    }

    @Override
    public List<VolunteerDTO> findVolunteersOrderByDistance(BigDecimal lat, BigDecimal lng) {
        Point location = Volunteer.createPoint(lat, lng);
        if (location == null) {
            return List.of();
        }

        return volunteerRepository.findVolunteersOrderByDistance(location).stream()
                .map(volunteerMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public VolunteerDTO createVolunteerProfile(UUID userId) {
        return createVolunteerProfileUseCase.execute(userId);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return volunteerRepository.existsByUserId(userId);
    }

    @Override
    public long countTotalVolunteers() {
        return volunteerRepository.count();
    }
}
