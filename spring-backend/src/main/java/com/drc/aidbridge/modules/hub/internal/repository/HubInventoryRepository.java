package com.drc.aidbridge.modules.hub.internal.repository;

import com.drc.aidbridge.modules.hub.internal.entity.HubInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HubInventoryRepository extends JpaRepository<HubInventory, UUID> {
    Optional<HubInventory> findByHubIdAndItemCategoryId(UUID hubId, UUID itemCategoryId);
}
