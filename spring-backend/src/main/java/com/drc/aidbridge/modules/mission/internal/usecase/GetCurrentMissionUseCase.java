package com.drc.aidbridge.modules.mission.internal.usecase;

import com.drc.aidbridge.modules.aid.AidFacade;
import com.drc.aidbridge.modules.mission.MissionHistoryFullDTO;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.repository.projection.MissionHistoryFullProjection;
import com.drc.aidbridge.modules.sos.SosFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetCurrentMissionUseCase {

    private final MissionJpaRepository missionRepository;
    private final SosFacade sosFacade;
    private final AidFacade aidFacade;

    public Optional<MissionHistoryFullDTO> execute(UUID volunteerId) {
        return missionRepository.findCurrentFullMissionByVolunteerId(volunteerId)
                .map(projection -> MissionHistoryFullDTO.builder()
                        .id(projection.getId())
                        .sosRequestId(projection.getSosRequestId())
                        .aidRequestId(projection.getAidRequestId())
                        .volunteerId(projection.getVolunteerId())
                        .hubId(projection.getHubId())
                        .missionType(projection.getMissionType())
                        .status(projection.getStatus())
                        .qrCodeToken(projection.getQrCodeToken())
                        .priorityScore(projection.getPriorityScore())
                        .acceptedAt(projection.getAcceptedAt())
                        .pickedUpAt(projection.getPickedUpAt())
                        .startedAt(projection.getStartedAt())
                        .completedAt(projection.getCompletedAt())
                        .cancelledAt(projection.getCancelledAt())
                        .cancellationReason(projection.getCancellationReason())
                        .address(projection.getAddress())
                        .confirmationImageUrl(projection.getConfirmationImageUrl())
                        .imageUrl(projection.getImageUrl())
                        .comment(projection.getComment())
                        .createdAt(projection.getCreatedAt())
                        .updatedAt(projection.getUpdatedAt())
                        .victimLat(projection.getVictimLat())
                        .victimLng(projection.getVictimLng())
                        .radiusKm(projection.getRadiusKm())
                        .description(projection.getDescription())
                        .sosRequestDetail(projection.getSosRequestId() != null ? sosFacade.getSosRequestById(projection.getSosRequestId()).orElse(null) : null)
                        .aidRequestDetail(projection.getAidRequestId() != null ? aidFacade.getAidRequestById(projection.getAidRequestId()) : null)
                        .build());
    }
}
