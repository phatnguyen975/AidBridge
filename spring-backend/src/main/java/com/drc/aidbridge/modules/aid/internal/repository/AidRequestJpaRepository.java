package com.drc.aidbridge.modules.aid.internal.repository;

import com.drc.aidbridge.modules.aid.internal.entity.AidRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AidRequestJpaRepository extends JpaRepository<AidRequest, UUID> {
}
