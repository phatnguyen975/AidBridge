package com.drc.aidbridge.modules.hub.internal.repository;

import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.shared.enums.HubStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HubRepository extends JpaRepository<Hub, UUID> {
	List<Hub> findByStatus(HubStatus status);
}
