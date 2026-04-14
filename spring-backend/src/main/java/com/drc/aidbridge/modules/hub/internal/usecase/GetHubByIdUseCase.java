package com.drc.aidbridge.modules.hub.internal.usecase;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.internal.mapper.HubMapper;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetHubByIdUseCase {

    private final HubRepository hubRepository;
    private final HubMapper hubMapper;

    public HubDTO execute(UUID id) {
        return hubRepository.findById(id).map(hubMapper::toDTO).orElse(null);
    }
}
