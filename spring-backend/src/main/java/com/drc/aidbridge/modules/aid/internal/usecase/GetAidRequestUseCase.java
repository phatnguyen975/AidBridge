package com.drc.aidbridge.modules.aid.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.aid.internal.entity.AidRequest;
import com.drc.aidbridge.modules.aid.internal.entity.AidRequestItem;
import com.drc.aidbridge.modules.aid.internal.mapper.AidMapper;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestItemJpaRepository;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestJpaRepository;
import com.drc.aidbridge.modules.aid.internal.web.dto.AidRequestResponse;
import com.drc.aidbridge.modules.mission.MissionDTO;
import com.drc.aidbridge.modules.mission.MissionFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetAidRequestUseCase {

    private final AidRequestJpaRepository aidRequestRepository;
    private final AidRequestItemJpaRepository aidRequestItemRepository;
    private final MissionFacade missionFacade;
    private final AidMapper aidMapper;

    public AidRequestResponse execute(UUID aidRequestId) {
        AidRequest aidRequest = aidRequestRepository.findById(aidRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Aid request not found: " + aidRequestId));

        MissionDTO mission = missionFacade.findMissionByAidRequestId(aidRequest.getId()).orElse(null);
        List<AidRequestItem> items = aidRequestItemRepository.findByAidRequestId(aidRequest.getId());

        return aidMapper.toResponse(aidRequest, items, mission);
    }
}
