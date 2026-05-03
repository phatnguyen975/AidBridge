package com.drc.aidbridge.modules.routing.internal.web;

import com.drc.aidbridge.modules.routing.RoutingDTO;
import com.drc.aidbridge.modules.routing.internal.service.DangerousZoneService;
import com.drc.aidbridge.modules.routing.internal.usecase.CalculateRouteUseCase;
import com.drc.aidbridge.modules.routing.internal.web.dto.DangerousZoneCreateRequest;
import com.drc.aidbridge.modules.routing.internal.web.dto.DangerousZoneResponse;
import com.drc.aidbridge.modules.routing.internal.web.dto.RoutingRequest;
import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for GraphHopper routing endpoints.
 */
@RestController
@RequestMapping("/api/routing")
@RequiredArgsConstructor
public class RoutingController {

    private final CalculateRouteUseCase calculateRouteUseCase;
    private final DangerousZoneService dangerousZoneService;

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<RoutingDTO>> calculate(
            @Valid @RequestBody RoutingRequest request) {
        RoutingDTO result = calculateRouteUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success("Route calculated successfully", result));
    }


    @GetMapping("/dangerous-zones")
    public ResponseEntity<ApiResponse<List<DangerousZoneResponse>>> listZones() {
        var results = dangerousZoneService.getAllZones().stream()
                .map(dangerousZoneService::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Fetched all dangerous zones", results));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/dangerous-zones")
    public ResponseEntity<ApiResponse<DangerousZoneResponse>> createZone(
            @Valid @RequestBody DangerousZoneCreateRequest request) {
        var entity = dangerousZoneService.createZone(
                request.getName(), 
                request.getGeometry(), 
                request.getAdminId());
        return ResponseEntity.ok(ApiResponse.success("Dangerous zone created", dangerousZoneService.toDto(entity)));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/dangerous-zones/{id}")
    public ResponseEntity<ApiResponse<DangerousZoneResponse>> updateZone(
            @PathVariable UUID id,
            @RequestBody DangerousZoneCreateRequest request) {
        var entity = dangerousZoneService.updateZone(
                id, 
                request.getName(), 
                request.getGeometry());
        return ResponseEntity.ok(ApiResponse.success("Dangerous zone updated", dangerousZoneService.toDto(entity)));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/dangerous-zones/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteZone(@PathVariable UUID id) {
        dangerousZoneService.deleteZone(id);
        return ResponseEntity.ok(ApiResponse.success("Dangerous zone deleted", null));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Routing service is operational"));
    }
}
