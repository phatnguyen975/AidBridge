package com.drc.aidbridge.modules.mission;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface MissionFacade {

    MissionDTO getMissionById(UUID missionId);

    Optional<MissionDTO> findMissionBySosRequestId(UUID sosRequestId);

    Optional<MissionDTO> findMissionByAidRequestId(UUID aidRequestId);

    MissionDTO createDeliveryMission(UUID aidRequestId, BigDecimal lat, BigDecimal lng);

    Optional<MissionDTO> cancelMissionByAidRequestId(UUID aidRequestId, String reason);

    boolean existsById(UUID missionId);

    // Added to support SOS module creation
    MissionDTO createRescueMission(UUID sosRequestId, BigDecimal lat, BigDecimal lng);

    
}
