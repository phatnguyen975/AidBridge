package com.drc.aidbridge.modules.sos.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.mission.MissionDTO;
import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.sos.internal.entity.SosRequest;
import com.drc.aidbridge.modules.sos.internal.mapper.SosMapper;
import com.drc.aidbridge.modules.sos.internal.repository.SosJpaRepository;
import com.drc.aidbridge.modules.sos.internal.web.dto.SosRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetSosRequestUseCase {

    private final SosJpaRepository sosRequestRepository;
    private final MissionFacade missionFacade;
    private final SosMapper sosMapper;

    public SosRequestResponse execute(UUID id) {
        SosRequest sos = sosRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SOS request not found: " + id));
                
        // Retrieve mission via facade
        MissionDTO mission = missionFacade.findMissionBySosRequestId(sos.getId()).orElse(null);
        return sosMapper.toResponse(sos, mission);
    }
}
