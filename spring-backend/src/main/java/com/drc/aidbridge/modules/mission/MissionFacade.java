package com.drc.aidbridge.modules.mission;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.sos.SosDTO;
import com.drc.aidbridge.modules.aid.AidRequestDTO;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
public interface MissionFacade {

    MissionDTO getMissionById(UUID missionId);

    Optional<MissionDTO> findMissionBySosRequestId(UUID sosRequestId);

    Optional<MissionDTO> findMissionByAidRequestId(UUID aidRequestId);

    MissionDTO createDeliveryMission(UUID aidRequestId, BigDecimal lat, BigDecimal lng);

    Optional<MissionDTO> cancelMissionByAidRequestId(UUID aidRequestId, String reason);

    boolean existsById(UUID missionId);

    // Added to support SOS module creation
    MissionDTO createRescueMission(UUID sosRequestId, BigDecimal lat, BigDecimal lng);

    void updateVictimLocationForSos(UUID sosRequestId, BigDecimal lat, BigDecimal lng);

    void updateVictimLocationForAidRequest(UUID aidRequestId, BigDecimal lat, BigDecimal lng);

    Optional<DispatchAttemptDTO> getLatestDispatchAttempt(UUID volunteerId);

    Optional<DispatchAttemptDTO> getDispatchAttempt(UUID dispatchAttemptId);

    Optional<DispatchAttemptDTO> cancelDispatchAttempt(UUID dispatchAttemptId);

    Optional<DispatchAttemptDTO> acceptDispatchAttempt(UUID volunteerId, UUID dispatchAttemptId);

    Page<MissionHistoryDTO> findHistoryByVolunteerId(UUID volunteerId,  Pageable pageable);

    Page<MissionHistoryFullDTO> findFullHistoryByVolunteerId(UUID volunteerId, Pageable pageable);

    Optional<MissionHistoryFullDTO> getCurrentMissionByVolunteerId(UUID volunteerId);

    MissionDTO completeMission(UUID missionId, String notes);

    MissionDTO cancelMission(UUID missionId, String reason);

    long countMissionsInPeriod(Instant start, Instant end);

    List<SosDTO> findSosByStatusAndDateRange(MissionStatus status, Instant start, Instant end);

    List<AidRequestDTO> findAidByStatusAndDateRange(MissionStatus status, Instant start, Instant end);
}
