package com.drc.aidbridge.modules.routing.internal.web;

import com.drc.aidbridge.modules.routing.RoutingDTO;
import com.drc.aidbridge.modules.routing.internal.usecase.CalculateRouteUseCase;
import com.drc.aidbridge.modules.routing.internal.web.dto.RoutingRequest;
import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for GraphHopper routing endpoints.
 *
 * <ul>
 *   <li>POST /api/routing/calculate — compute route between two coordinates with single strategy and optional dangerous zones</li>
 *   <li>GET  /api/routing/health    — check if routing engine is operational</li>
 * </ul>
 *
 * Supported strategies: urgent_response, disaster_safe, heavy_aid, community_delivery, offroad_terrain
 */
@RestController
@RequestMapping("/api/routing")
@RequiredArgsConstructor
public class RoutingController {

    private final CalculateRouteUseCase calculateRouteUseCase;

    /**
     * POST /api/routing/calculate
     * Calculates the driving route between start and end coordinates.
     */
    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<RoutingDTO>> calculate(
            @Valid @RequestBody RoutingRequest request) {
        System.out.println("Received routing request: " + request);
        RoutingDTO result = calculateRouteUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success("Route calculated successfully", result));
    }

    /**
     * GET /api/routing/health
     * Quick health check — if this responds, GraphHopper bean is initialized.
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Routing service is operational"));
    }
}
