package com.drc.aidbridge.modules.aid.internal.usecase;

import com.drc.aidbridge.modules.aid.internal.entity.AidRequest;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestJpaRepository;
import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.shared.dto.UpdateRequestLocationRequest;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateAidRequestLocationUseCase {

    private final AidRequestJpaRepository aidRequestJpaRepository;
    private final MissionFacade missionFacade;

    @Transactional
    public void execute(UUID requesterId, UUID aidRequestId, UpdateRequestLocationRequest request) {
        AidRequest aidRequest = aidRequestJpaRepository.findByIdAndRequesterId(aidRequestId, requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Aid request not found: " + aidRequestId));

        aidRequest.setLocation(AidRequest.createPoint(
            request.getLat().doubleValue(),
            request.getLng().doubleValue()
        ));
        aidRequestJpaRepository.save(aidRequest);

        missionFacade.updateVictimLocationForAidRequest(aidRequestId, request.getLat(), request.getLng());
        log.info("Updated aid request location for aidRequestId={} requesterId={}", aidRequestId, requesterId);
    }
}
