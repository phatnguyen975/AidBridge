package com.drc.aidbridge.modules.sos.internal.repository;
// ... existing code ...
import com.drc.aidbridge.modules.sos.internal.entity.SosRequest;
import com.drc.aidbridge.modules.shared.enums.SosStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SosJpaRepository extends JpaRepository<SosRequest, UUID> {
    List<SosRequest> findByRequesterId(UUID requesterId);
    List<SosRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);
    List<SosRequest> findByRequesterIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(UUID requesterId,
                                                                                        Instant createdAt);
    Optional<SosRequest> findByClientRequestId(String clientRequestId);
    Optional<SosRequest> findByIdAndRequesterId(UUID id, UUID requesterId);
    List<SosRequest> findByStatus(SosStatus status);

    List<SosRequest> findByStatusAndCreatedAtBetween(SosStatus status, Instant start, Instant end);
}
