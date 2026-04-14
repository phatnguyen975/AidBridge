package com.drc.aidbridge.modules.aid.internal.repository;

import com.drc.aidbridge.modules.aid.internal.entity.AidItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AidItemCategoryJpaRepository extends JpaRepository<AidItemCategory, UUID> {
    List<AidItemCategory> findByIsLeafTrue();
    List<AidItemCategory> findByIsLeafFalse();
}
