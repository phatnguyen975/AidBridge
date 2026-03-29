package com.drc.aidbridge.modules.aid.internal.usecase;

import com.drc.aidbridge.entity.enums.AidStatus;
import com.drc.aidbridge.entity.enums.MissionStatus;
import com.drc.aidbridge.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.aid.internal.entity.AidRequest;
import com.drc.aidbridge.modules.aid.internal.entity.AidRequestItem;
import com.drc.aidbridge.modules.aid.internal.mapper.AidMapper;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestItemJpaRepository;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestJpaRepository;
import com.drc.aidbridge.modules.aid.internal.web.dto.AidRequestResponse;
import com.drc.aidbridge.modules.aid.internal.web.dto.CancelAidRequest;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import com.drc.aidbridge.entity.Mission;
import com.drc.aidbridge.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CancelAidRequestUseCase {

    private final AidRequestJpaRepository aidRequestRepository;
    private final AidRequestItemJpaRepository aidRequestItemRepository;
    private final MissionRepository missionRepository;
    private final UserFacade userFacade;
    private final AidMapper aidMapper;

    @Transactional
    public AidRequestResponse execute(UUID requesterId, UUID aidRequestId, CancelAidRequest request) {
        UserDTO requester = userFacade.getUserById(requesterId);

        AidRequest aidRequest = aidRequestRepository.findById(aidRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Aid request not found: " + aidRequestId));

        // Only the original requester or an ADMIN can cancel
        if (!requesterId.equals(aidRequest.getRequesterId()) && !"ADMIN".equals(requester.getRole())) {
            throw new AccessDeniedException("User is not allowed to cancel this aid request");
        }

        aidRequest.setStatus(AidStatus.CANCELLED);
        AidRequest saved = aidRequestRepository.save(aidRequest);

        Mission mission = missionRepository.findByAidRequestId(saved.getId()).orElse(null);
        if (mission != null && mission.getStatus() != MissionStatus.COMPLETED && mission.getStatus() != MissionStatus.CANCELLED) {
            mission.setStatus(MissionStatus.CANCELLED);
            mission.setCancelledAt(Instant.now());
            mission.setCancellationReason(request.getReason());
            missionRepository.save(mission);
        }

        List<AidRequestItem> items = aidRequestItemRepository.findByAidRequestId(saved.getId());
        return aidMapper.toResponse(saved, items, mission);
    }
}
