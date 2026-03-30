package com.drc.aidbridge.modules.volunteer.internal.repository;

import com.drc.aidbridge.modules.volunteer.internal.entity.Volunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VolunteerJpaRepository extends JpaRepository<Volunteer, UUID> {
    Optional<Volunteer> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}
