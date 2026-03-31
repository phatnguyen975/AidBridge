package com.drc.aidbridge.modules.sos.internal.usecase;

import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.sos.internal.mapper.SosMapper;
import com.drc.aidbridge.modules.sos.internal.repository.SosJpaRepository;
import com.drc.aidbridge.modules.sos.internal.web.dto.SosRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ListSosRequestsUseCase {

    private final SosJpaRepository sosRequestRepository;
    private final MissionFacade missionFacade;
    private final SosMapper sosMapper;

    public List<SosRequestResponse> execute() {
        return sosRequestRepository.findAll().stream()
                .map(req -> missionFacade.findMissionBySosRequestId(req.getId())
                        .map(m -> sosMapper.toResponse(req, m))
                        .orElseGet(() -> sosMapper.toResponse(req, null)))
                .collect(Collectors.toList());
    }
}
