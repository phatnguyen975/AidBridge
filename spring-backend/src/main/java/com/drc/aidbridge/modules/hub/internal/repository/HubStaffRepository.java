package com.drc.aidbridge.modules.hub.internal.repository;

import com.drc.aidbridge.modules.hub.internal.entity.HubStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HubStaffRepository extends JpaRepository<HubStaff, UUID> {
    List<HubStaff> findByHubIdAndUnassignedAtIsNull(UUID hubId);
    Optional<HubStaff> findByHubIdAndUserIdAndUnassignedAtIsNull(UUID hubId, UUID userId);
}
