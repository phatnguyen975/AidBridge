package com.drc.aidbridge.modules.hub.internal;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.HubFacade;
import com.drc.aidbridge.modules.hub.internal.usecase.GetHubByIdUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HubFacadeImpl implements HubFacade {

    private final GetHubByIdUseCase getHubByIdUseCase;

    @Override
    public HubDTO getById(UUID id) {
        return getHubByIdUseCase.execute(id);
    }
}
