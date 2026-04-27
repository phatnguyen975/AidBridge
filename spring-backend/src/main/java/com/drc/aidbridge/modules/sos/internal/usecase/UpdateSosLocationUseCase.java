package com.drc.aidbridge.modules.sos.internal.usecase;

import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.shared.dto.UpdateRequestLocationRequest;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.sos.internal.entity.SosRequest;
import com.drc.aidbridge.modules.sos.internal.repository.SosJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateSosLocationUseCase {

    private final SosJpaRepository sosJpaRepository;
    private final MissionFacade missionFacade;

    @Transactional
    public void execute(UUID requesterId, UUID sosRequestId, UpdateRequestLocationRequest request) {
        SosRequest sosRequest = sosJpaRepository.findByIdAndRequesterId(sosRequestId, requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("SOS request not found: " + sosRequestId));

        sosRequest.setLocation(SosRequest.createPoint(
            request.getLat().doubleValue(),
            request.getLng().doubleValue()
        ));
        sosJpaRepository.save(sosRequest);

        missionFacade.updateVictimLocationForSos(sosRequestId, request.getLat(), request.getLng());
        log.info("Updated SOS location for sosRequestId={} requesterId={}", sosRequestId, requesterId);
    }
}
