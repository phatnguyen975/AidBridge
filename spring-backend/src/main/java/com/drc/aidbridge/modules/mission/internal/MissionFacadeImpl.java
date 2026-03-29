package com.drc.aidbridge.modules.mission.internal;

import com.drc.aidbridge.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.mission.MissionDTO;
import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.mission.internal.mapper.MissionMapper;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    public boolean existsById(UUID missionId) {
        return missionRepository.existsById(missionId);
    }
}
