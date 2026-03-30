package com.drc.aidbridge.modules.volunteer;

import java.util.Optional;
import java.util.UUID;

public interface VolunteerFacade {
    Optional<VolunteerDTO> getVolunteerByUserId(UUID userId);
    VolunteerDTO createVolunteerProfile(UUID userId);
    boolean existsByUserId(UUID userId);
}
