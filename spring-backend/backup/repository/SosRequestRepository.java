package com.drc.aidbridge.repository;

import com.drc.aidbridge.entity.SosRequest;
import com.drc.aidbridge.entity.enums.SosStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SosRequestRepository extends JpaRepository<SosRequest, UUID> {

    List<SosRequest> findByRequesterId(UUID requesterId);

    List<SosRequest> findByStatus(SosStatus status);
}
