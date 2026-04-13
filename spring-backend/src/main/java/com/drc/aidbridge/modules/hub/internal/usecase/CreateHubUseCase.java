package com.drc.aidbridge.modules.hub.internal.usecase;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.mapper.HubMapper;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.hub.internal.web.dto.CreateHubRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateHubUseCase {

    private final HubRepository hubRepository;
    private final HubMapper hubMapper;

    @Transactional
    public HubDTO execute(CreateHubRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is null");
        }
        Hub hub = hubMapper.toEntity(request);
        Hub saved = hubRepository.save(hub);
        return hubMapper.toDTO(saved);
    }
}