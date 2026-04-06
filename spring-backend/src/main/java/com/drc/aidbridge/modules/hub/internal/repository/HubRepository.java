package com.drc.aidbridge.modules.hub.internal.repository;

import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HubRepository extends JpaRepository<Hub, UUID> {
}
