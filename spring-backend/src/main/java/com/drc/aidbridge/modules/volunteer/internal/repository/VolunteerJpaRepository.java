package com.drc.aidbridge.modules.volunteer.internal.repository;

import com.drc.aidbridge.modules.volunteer.internal.entity.Volunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VolunteerJpaRepository extends JpaRepository<Volunteer, UUID> {
    Optional<Volunteer> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);

    // Batch update: Mark volunteers offline if no heartbeat received within timeout
    @Modifying
    @Transactional
    @Query("UPDATE Volunteer v SET v.isOnline = false WHERE v.isOnline = true AND v.lastActiveAt < :cutoffTime")
    int updateOfflineVolunteers(@Param("cutoffTime") Instant cutoffTime);
}
