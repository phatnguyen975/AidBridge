package com.drc.aidbridge.repository;

import com.drc.aidbridge.entity.AidRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AidRequestItemRepository extends JpaRepository<AidRequestItem, UUID> {
    List<AidRequestItem> findByAidRequestId(UUID aidRequestId);
}
