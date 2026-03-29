package com.drc.aidbridge.modules.mission;

import java.util.Optional;
import java.util.UUID;

public interface MissionFacade {

    MissionDTO getMissionById(UUID missionId);

    Optional<MissionDTO> findMissionBySosRequestId(UUID sosRequestId);

    Optional<MissionDTO> findMissionByAidRequestId(UUID aidRequestId);

    boolean existsById(UUID missionId);
}
