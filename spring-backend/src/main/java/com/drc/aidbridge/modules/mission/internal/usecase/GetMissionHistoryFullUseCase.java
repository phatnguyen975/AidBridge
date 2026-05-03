package com.drc.aidbridge.modules.mission.internal.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.drc.aidbridge.modules.mission.MissionHistoryFullDTO;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.sos.SosFacade;
import com.drc.aidbridge.modules.aid.AidFacade;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetMissionHistoryFullUseCase {

    private final MissionJpaRepository missionRepository;
    private final SosFacade sosFacade;
    private final AidFacade aidFacade;

    @Transactional(readOnly = true)
    public Page<MissionHistoryFullDTO> execute(UUID volunteerId, Pageable pageable) {
        return missionRepository.findFullHistoryByVolunteerId(volunteerId, pageable)
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
