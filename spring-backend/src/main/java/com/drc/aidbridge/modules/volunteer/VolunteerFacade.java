package com.drc.aidbridge.modules.volunteer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VolunteerFacade {
    Optional<VolunteerDTO> getVolunteerByUserId(UUID userId);
    List<VolunteerDTO> findNearbyVolunteers(BigDecimal lat, BigDecimal lng);
    List<VolunteerDTO> findVolunteersOrderByDistance(BigDecimal lat, BigDecimal lng);
    VolunteerDTO createVolunteerProfile(UUID userId);
    boolean existsByUserId(UUID userId);
    long countTotalVolunteers();
}
