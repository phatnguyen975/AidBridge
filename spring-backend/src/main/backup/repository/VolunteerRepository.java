package com.drc.aidbridge.repository;

import com.drc.aidbridge.entity.Volunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for volunteer profiles.
 */
@Repository
public interface VolunteerRepository extends JpaRepository<Volunteer, UUID> {

    /**
     * Find volunteer profile by user ID.
     *
     * @param userId The user ID
     * @return Volunteer profile if exists
     */
    Optional<Volunteer> findByUserId(UUID userId);

    /**
     * Check if volunteer profile exists for user.
     *
     * @param userId The user ID
     * @return true if profile exists
     */
    boolean existsByUserId(UUID userId);
}
