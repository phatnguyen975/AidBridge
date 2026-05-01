package com.drc.aidbridge.modules.aid.internal.repository;

import com.drc.aidbridge.modules.aid.internal.entity.AidRequest;
import com.drc.aidbridge.modules.shared.enums.AidStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AidRequestJpaRepository extends JpaRepository<AidRequest, UUID> {
	List<AidRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);

	List<AidRequest> findByRequesterIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(UUID requesterId,
																						Instant createdAt);

	Optional<AidRequest> findByIdAndRequesterId(UUID id, UUID requesterId);

	List<AidRequest> findByStatus(AidStatus status);

	List<AidRequest> findByStatusAndCreatedAtBetween(AidStatus status, Instant start, Instant end);
}
