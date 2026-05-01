package com.drc.aidbridge.modules.aid;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import com.drc.aidbridge.modules.shared.enums.AidStatus;
import  java.time.Instant;
public interface AidFacade {

    AidRequestDTO getAidRequestById(UUID aidRequestId);

    boolean existsById(UUID aidRequestId);

    List<AidItemCategoryDTO> findAllCategoriesById(Collection<UUID> ids);

    List<AidRequestDTO> findAllByStatus(AidStatus status);

    List<AidRequestDTO> findAllByStatusAndDateRange(AidStatus status, Instant start, Instant end);
}
