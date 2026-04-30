package com.drc.aidbridge.modules.hub.internal;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.HubFacade;
import com.drc.aidbridge.modules.hub.internal.usecase.CreateHubUseCase;
import com.drc.aidbridge.modules.hub.internal.usecase.GetHubByIdUseCase;
import com.drc.aidbridge.modules.hub.internal.usecase.ListHubsUseCase;
import com.drc.aidbridge.modules.hub.internal.usecase.StockInHubInventoryUseCase;
import com.drc.aidbridge.modules.hub.internal.usecase.UpdateHubUseCase;
import com.drc.aidbridge.modules.hub.internal.usecase.ListHubNearLocationUseCase;
import com.drc.aidbridge.modules.hub.internal.web.dto.CreateHubRequest;
import com.drc.aidbridge.modules.hub.internal.web.dto.StockInHubInventoryRequest;
import com.drc.aidbridge.modules.hub.internal.web.dto.UpdateHubRequest;
import com.drc.aidbridge.modules.shared.enums.HubStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HubFacadeImpl implements HubFacade {

    private final ListHubsUseCase listHubsUseCase;
    private final GetHubByIdUseCase getHubByIdUseCase;
    private final CreateHubUseCase createHubUseCase;
    private final UpdateHubUseCase updateHubUseCase;
    private final ListHubNearLocationUseCase listHubNearLocationUseCase;
    private final StockInHubInventoryUseCase stockInHubInventoryUseCase;

    @Override
    public HubDTO getById(UUID id) {
        return getHubByIdUseCase.execute(id);
    }

    @Override
    public List<HubDTO> list(HubStatus status) {
        return listHubsUseCase.execute(status);
    }

    @Override
    public List<HubDTO> list(HubStatus status, String keyword) {
        return listHubsUseCase.execute(status, keyword);
    }

    @Override
    public HubDTO create(CreateHubRequest request) {
        return createHubUseCase.execute(request);
    }

    @Override
    public HubDTO update(UUID id, UpdateHubRequest request) {
        return updateHubUseCase.execute(id, request);
    }

    @Override
    public HubDTO stockIn(UUID id, StockInHubInventoryRequest request) {
        return stockInHubInventoryUseCase.execute(id, request);
    }

    @Override 
    public List<HubDTO> listNearLocation(HubStatus status, double lat, double lon, double radius) {
        return listHubNearLocationUseCase.execute(status, lat, lon, radius);
    }
}
