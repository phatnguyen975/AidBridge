package com.drc.aidbridge.modules.mission.internal;

import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import com.drc.aidbridge.modules.mission.MissionDTO;
import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.locationtech.jts.geom.Point;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import com.drc.aidbridge.modules.shared.enums.DispatchResponse;
import com.drc.aidbridge.modules.mission.DispatchAttemptDTO;
import com.drc.aidbridge.modules.mission.internal.repository.DispatchAttemptJpaRepository;
import com.drc.aidbridge.modules.mission.internal.usecase.AcceptMissionUseCase;
import com.drc.aidbridge.modules.mission.internal.usecase.GetMissionHistoryUseCase;
import com.drc.aidbridge.modules.mission.internal.usecase.GetMissionHistoryFullUseCase;
import com.drc.aidbridge.modules.mission.internal.usecase.GetCurrentMissionUseCase;
import com.drc.aidbridge.modules.mission.MissionHistoryDTO;
import com.drc.aidbridge.modules.mission.MissionHistoryFullDTO;
import com.drc.aidbridge.modules.mission.internal.web.dto.AcceptMissionRequest;
import com.drc.aidbridge.modules.mission.internal.usecase.CompleteMissionUseCase;
import com.drc.aidbridge.modules.mission.internal.usecase.CancelMissionUseCase;
import com.drc.aidbridge.modules.mission.internal.web.dto.CompleteMissionRequest;
import com.drc.aidbridge.modules.mission.internal.web.dto.CancelMissionRequest;
import com.drc.aidbridge.modules.mission.internal.web.dto.MissionResponse;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import com.drc.aidbridge.modules.sos.SosDTO;
import com.drc.aidbridge.modules.aid.AidRequestDTO;
import com.drc.aidbridge.modules.sos.internal.mapper.SosMapper;
import com.drc.aidbridge.modules.aid.internal.mapper.AidMapper;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissionFacadeImpl implements MissionFacade {

    private final MissionJpaRepository missionRepository;
    private final MissionMapper missionMapper;
    private final DispatchAttemptJpaRepository dispatchAttemptRepository;
    private final AcceptMissionUseCase acceptMissionUseCase;
    private final GetMissionHistoryUseCase getMissionHistoryUseCase;
    private final GetMissionHistoryFullUseCase getMissionHistoryFullUseCase;
    private final GetCurrentMissionUseCase getCurrentMissionUseCase;
    private final CompleteMissionUseCase completeMissionUseCase;
    private final CancelMissionUseCase cancelMissionUseCase;
    private final SosMapper sosMapper;
    private final AidMapper aidMapper;

    @Override
    public MissionDTO getMissionById(UUID missionId) {
        return missionRepository.findById(missionId)
                .map(missionMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found: " + missionId));
    }

    @Override
    public Optional<MissionDTO> findMissionBySosRequestId(UUID sosRequestId) {
        return missionRepository.findBySosRequestId(sosRequestId)
                .map(missionMapper::toDTO);
    }

    @Override
    public Optional<MissionDTO> findMissionByAidRequestId(UUID aidRequestId) {
        return missionRepository.findByAidRequestId(aidRequestId)
                .map(missionMapper::toDTO);
    }

    @Override
    public MissionDTO createDeliveryMission(UUID aidRequestId, BigDecimal lat, BigDecimal lng) {
        Mission mission = Mission.builder()
                .missionType(MissionType.DELIVERY)
                .aidRequestId(aidRequestId)
                .codeName(generateMissionCodeName())
                .status(MissionStatus.PENDING)
                .victimLocation(Mission.createPoint(lat, lng))
                .build();
        return missionMapper.toDTO(missionRepository.save(mission));
    }

    @Override
    public Optional<MissionDTO> cancelMissionByAidRequestId(UUID aidRequestId, String reason) {
        return missionRepository.findByAidRequestId(aidRequestId)
                .map(mission -> {
                    mission.setStatus(MissionStatus.CANCELLED);
                    mission.setCancelledAt(Instant.now());
                    mission.setCancellationReason(reason);
                    return missionMapper.toDTO(missionRepository.save(mission));
                });
    }

    @Override
    public boolean existsById(UUID missionId) {
        return missionRepository.existsById(missionId);
    }

    @Override
    public MissionDTO createRescueMission(UUID sosRequestId, BigDecimal lat, BigDecimal lng) {

        if (lat == null || lng == null) {
            throw new IllegalArgumentException("Mission location must not be null");
        }

        Point location = Mission.createPoint(lat, lng);

        Mission mission = Mission.builder()
                .missionType(MissionType.RESCUE)
                .status(MissionStatus.PENDING)
                .sosRequestId(sosRequestId)
                .codeName(generateMissionCodeName())
                .victimLocation(location)
                .build();

        Mission saved = missionRepository.save(mission);

        return missionMapper.toDTO(saved);
    }

    @Override
    public void updateVictimLocationForSos(UUID sosRequestId, BigDecimal lat, BigDecimal lng) {
        if (sosRequestId == null || lat == null || lng == null) {
            return;
        }

        missionRepository.findBySosRequestId(sosRequestId).ifPresent(mission -> {
            mission.setVictimLocation(Mission.createPoint(lat, lng));
            missionRepository.save(mission);
        });
    }

    @Override
    public void updateVictimLocationForAidRequest(UUID aidRequestId, BigDecimal lat, BigDecimal lng) {
        if (aidRequestId == null || lat == null || lng == null) {
            return;
        }

        missionRepository.findByAidRequestId(aidRequestId).ifPresent(mission -> {
            mission.setVictimLocation(Mission.createPoint(lat, lng));
            missionRepository.save(mission);
        });
    }

    @Override
    public Optional<DispatchAttemptDTO> getLatestDispatchAttempt(UUID volunteerId) {
        return dispatchAttemptRepository
                .findTopByVolunteerIdAndResponseOrderByCreatedAtDesc(volunteerId, DispatchResponse.PENDING)
                .map(entity -> DispatchAttemptDTO.builder()
                        .id(entity.getId())
                        .missionId(entity.getMissionId())
                        .volunteerId(entity.getVolunteerId())
                        .dispatchType(entity.getDispatchType() != null ? entity.getDispatchType().name() : null)
                        .batchNumber(entity.getBatchNumber())
                        .radiusKm(entity.getRadiusKm())
                        .response(entity.getResponse() != null ? entity.getResponse().name() : null)
                        .build());
    }

    @Override
    public Optional<DispatchAttemptDTO> getDispatchAttempt(UUID dispatchAttemptId) {
        return dispatchAttemptRepository.findById(dispatchAttemptId)
                .map(entity -> DispatchAttemptDTO.builder()
                        .id(entity.getId())
                        .missionId(entity.getMissionId())
                        .volunteerId(entity.getVolunteerId())
                        .dispatchType(entity.getDispatchType() != null ? entity.getDispatchType().name() : null)
                        .batchNumber(entity.getBatchNumber())
                        .radiusKm(entity.getRadiusKm())
                        .response(entity.getResponse() != null ? entity.getResponse().name() : null)
                        .build());
    }

    @Override
    public Optional<DispatchAttemptDTO> cancelDispatchAttempt(UUID dispatchAttemptId) {
        return dispatchAttemptRepository.findById(dispatchAttemptId)
                .map(entity -> {
                    entity.setResponse(DispatchResponse.REJECTED);
                    entity.setRespondedAt(Instant.now());
                    return dispatchAttemptRepository.save(entity);
                })
                .map(entity -> DispatchAttemptDTO.builder()
                        .id(entity.getId())
                        .missionId(entity.getMissionId())
                        .volunteerId(entity.getVolunteerId())
                        .dispatchType(entity.getDispatchType() != null ? entity.getDispatchType().name() : null)
                        .batchNumber(entity.getBatchNumber())
                        .radiusKm(entity.getRadiusKm())
                        .response(entity.getResponse() != null ? entity.getResponse().name() : null)
                        .build());
    }

    @Override
    @Transactional
    public Optional<DispatchAttemptDTO> acceptDispatchAttempt(UUID volunteerId, UUID dispatchAttemptId) {
        return dispatchAttemptRepository.findById(dispatchAttemptId)
                .map(entity -> {
                    AcceptMissionRequest request = AcceptMissionRequest.builder()
                            .dispatchAttemptId(dispatchAttemptId)
                            .build();

                    // Uỷ thác toàn bộ nghiệp vụ và xử lý cache cho UseCase
                    acceptMissionUseCase.execute(entity.getMissionId(), volunteerId, request);

                    return DispatchAttemptDTO.builder()
                            .id(entity.getId())
                            .missionId(entity.getMissionId())
                            .volunteerId(entity.getVolunteerId())
                            .dispatchType(entity.getDispatchType() != null ? entity.getDispatchType().name() : null)
                            .batchNumber(entity.getBatchNumber())
                            .radiusKm(entity.getRadiusKm())
                            .response(entity.getResponse() != null ? entity.getResponse().name() : null)
                            .build();
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MissionHistoryDTO> findHistoryByVolunteerId(UUID volunteerId, Pageable pageable) {
        return getMissionHistoryUseCase.execute(volunteerId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MissionHistoryFullDTO> findFullHistoryByVolunteerId(UUID volunteerId, Pageable pageable) {
        return getMissionHistoryFullUseCase.execute(volunteerId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MissionHistoryFullDTO> getCurrentMissionByVolunteerId(UUID volunteerId) {
        return getCurrentMissionUseCase.execute(volunteerId);
    }

    @Override
    @Transactional
    public MissionDTO completeMission(UUID missionId, String notes) {
        CompleteMissionRequest request = CompleteMissionRequest.builder().notes(notes).build();
        MissionResponse response = completeMissionUseCase.execute(missionId, null, request);
        return MissionDTO.builder()
                .id(response.getId())
                .missionType(response.getMissionType())
                .status(response.getStatus())
                .sosRequestId(response.getSosRequestId())
                .aidRequestId(response.getAidRequestId())
                .volunteerId(response.getVolunteerId())
                .hubId(response.getHubId())
                .codeName(response.getCodeName())
                .victimLat(response.getVictimLat())
                .victimLng(response.getVictimLng())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }

    @Override
    public MissionDTO cancelMission(UUID missionId, String reason) {
        CancelMissionRequest request = CancelMissionRequest.builder().cancellationReason(reason).build();
        MissionResponse response = cancelMissionUseCase.execute(missionId, request);
        return MissionDTO.builder()
                .id(response.getId())
                .missionType(response.getMissionType())
                .status(response.getStatus())
                .sosRequestId(response.getSosRequestId())
                .aidRequestId(response.getAidRequestId())
                .volunteerId(response.getVolunteerId())
                .hubId(response.getHubId())
                .codeName(response.getCodeName())
                .victimLat(response.getVictimLat())
                .victimLng(response.getVictimLng())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }

    @Override
    public long countMissionsInPeriod(Instant start, Instant end) {
        return missionRepository.countByCreatedAtBetween(start, end);
    }

    @Override
    public List<SosDTO> findSosByStatusAndDateRange(MissionStatus status, Instant start, Instant end) {
        return missionRepository.findSosByMissionStatus(status, start, end)
                .stream()
                .map(sosMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AidRequestDTO> findAidByStatusAndDateRange(MissionStatus status, Instant start, Instant end) {
        return missionRepository.findAidByMissionStatus(status, start, end)
                .stream()
                .map(aidMapper::toDTO)
                .collect(Collectors.toList());
    }

    private String generateMissionCodeName() {
    String timePart = String.valueOf(System.currentTimeMillis()).substring(7);
    String randomPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    return "MSN-" + timePart + "-" + randomPart;
    }
}
