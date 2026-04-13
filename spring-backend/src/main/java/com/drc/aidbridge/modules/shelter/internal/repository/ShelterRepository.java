package com.drc.aidbridge.modules.shelter.internal.repository;

import com.drc.aidbridge.modules.shelter.internal.entity.Shelter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShelterRepository extends JpaRepository<Shelter, UUID> {
    List<Shelter> findByIsActiveTrue();
    List<Shelter> findByHubId(UUID hubId);
}
