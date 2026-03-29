package com.drc.aidbridge.modules.aid;

import java.util.UUID;

public interface AidFacade {

    AidRequestDTO getAidRequestById(UUID aidRequestId);

    boolean existsById(UUID aidRequestId);
}
