package com.drc.aidbridge.modules.hub.internal.usecase;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.internal.mapper.HubMapper;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.shared.enums.HubStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListHubsUseCase {

    private final HubRepository hubRepository;
    private final HubMapper hubMapper;

    public List<HubDTO> execute(HubStatus status) {
        if (status != null) {
            return hubRepository.findByStatus(status).stream().map(hubMapper::toDTO).toList();
        }
        return hubRepository.findAll().stream().map(hubMapper::toDTO).toList();
    }
}