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
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MissionFacadeImpl implements MissionFacade {

    private final MissionJpaRepository missionRepository;
    private final MissionMapper missionMapper;

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
}
