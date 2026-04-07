package com.drc.aidbridge.modules.sponsor.internal.repository;

import com.drc.aidbridge.modules.sponsor.internal.entity.Sponsor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SponsorJpaRepository extends JpaRepository<Sponsor, UUID> {
    Optional<Sponsor> findByUserId(UUID userId);
}
