package com.drc.aidbridge.repository;

import com.drc.aidbridge.entity.AidRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AidRequestRepository extends JpaRepository<AidRequest, UUID> {
}
