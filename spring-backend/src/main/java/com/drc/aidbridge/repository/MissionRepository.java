package com.drc.aidbridge.repository;

import com.drc.aidbridge.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MissionRepository extends JpaRepository<Mission, UUID> {

    java.util.Optional<Mission> findBySosRequestId(UUID sosRequestId);
}
