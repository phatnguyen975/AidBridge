package com.drc.aidbridge.modules.aid.internal.repository;

import com.drc.aidbridge.modules.aid.internal.entity.AidRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AidRequestItemJpaRepository extends JpaRepository<AidRequestItem, UUID> {
    List<AidRequestItem> findByAidRequestId(UUID aidRequestId);

    List<AidRequestItem> findByAidRequestIdIn(List<UUID> aidRequestIds);
}
