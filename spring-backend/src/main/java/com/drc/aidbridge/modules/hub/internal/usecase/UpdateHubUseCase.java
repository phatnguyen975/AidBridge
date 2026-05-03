package com.drc.aidbridge.modules.hub.internal.usecase;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.mapper.HubMapper;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.hub.internal.web.dto.UpdateHubRequest;
import com.drc.aidbridge.modules.shared.enums.HubStatus;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateHubUseCase {

    private final HubRepository hubRepository;
    private final HubMapper hubMapper;

    @Transactional
    public HubDTO execute(UUID id, UpdateHubRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is null");
        }

        Hub hub = hubRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hub not found: " + id));

        validateStatus(request.getStatus());
        hubMapper.patchEntity(hub, request);
        Hub saved = hubRepository.save(hub);
        return hubMapper.toDTO(saved);
    }

    private void validateStatus(HubStatus status) {
        if (status == null) {
            return;
        }
        if (status != HubStatus.ACTIVE && status != HubStatus.INACTIVE) {
            throw new IllegalArgumentException("Hub status must be ACTIVE or INACTIVE");
        }
    }
}
