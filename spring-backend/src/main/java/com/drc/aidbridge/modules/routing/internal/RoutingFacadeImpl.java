package com.drc.aidbridge.modules.routing.internal;

import com.drc.aidbridge.modules.routing.RoutingDTO;
import com.drc.aidbridge.modules.routing.RoutingFacade;
import com.drc.aidbridge.modules.routing.internal.usecase.CalculateRouteUseCase;
import com.drc.aidbridge.modules.routing.internal.web.dto.RoutingRequest;
import com.graphhopper.GraphHopper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Facade implementation — delegates to use cases.
 * This is the ONLY class other modules should interact with (via RoutingFacade interface).
 */
@Service
@RequiredArgsConstructor
public class RoutingFacadeImpl implements RoutingFacade {

    private final CalculateRouteUseCase calculateRouteUseCase;
    private final GraphHopper graphHopper;

    @Override
    public RoutingDTO calculateRoute(double startLat, double startLon,
                                     double endLat, double endLon) {
        RoutingRequest request = RoutingRequest.builder()
                .startLat(startLat)
                .startLon(startLon)
                .endLat(endLat)
                .endLon(endLon)
                .build();
        return calculateRouteUseCase.execute(request);
    }

    @Override
    public boolean isReady() {
        try {
            return graphHopper.getFullyLoaded();
        } catch (Exception e) {
            return false;
        }
    }
}
