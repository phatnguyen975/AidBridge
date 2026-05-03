package com.drc.aidbridge.modules.routing.internal.repository;

import com.drc.aidbridge.modules.routing.internal.entity.DangerousZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DangerousZoneRepository extends JpaRepository<DangerousZone, UUID> {
}
