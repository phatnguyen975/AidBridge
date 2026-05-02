package com.drc.aidbridge.modules.sos;

import com.drc.aidbridge.modules.shared.enums.SosStatus;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.util.List;
public interface SosFacade {
    Optional<SosDTO> getSosRequestById(UUID id);

    void updateStatus(UUID id, SosStatus status);

    boolean existsById(UUID id);

    List<SosDTO> findAllByStatus(SosStatus status);

    List<SosDTO> findAllByStatusAndDateRange(SosStatus status, Instant start, Instant end);
}
