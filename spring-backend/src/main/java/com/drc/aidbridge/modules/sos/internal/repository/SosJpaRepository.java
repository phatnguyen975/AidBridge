package com.drc.aidbridge.modules.sos.internal.repository;
// ... existing code ...
import com.drc.aidbridge.modules.sos.internal.entity.SosRequest;
import com.drc.aidbridge.modules.shared.enums.SosStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SosJpaRepository extends JpaRepository<SosRequest, UUID> {
    List<SosRequest> findByRequesterId(UUID requesterId);
    List<SosRequest> findByStatus(SosStatus status);
}
