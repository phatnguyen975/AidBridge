package com.drc.aidbridge.modules.hub.internal.web;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.HubFacade;
import com.drc.aidbridge.modules.hub.internal.usecase.GetHubByIdUseCase;
import com.drc.aidbridge.modules.hub.internal.web.dto.CreateHubRequest;
import com.drc.aidbridge.modules.hub.internal.web.dto.StockInHubInventoryRequest;
import com.drc.aidbridge.modules.hub.internal.web.dto.UpdateHubRequest;
import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.shared.enums.HubStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hubs")
@RequiredArgsConstructor
public class HubController {

    private final GetHubByIdUseCase getHubByIdUseCase;
    private final HubFacade hubFacade;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HubDTO>>> listHubs(@RequestParam(required = false) HubStatus status,
                                                              @RequestParam(required = false) String keyword) {
        List<HubDTO> list = hubFacade.list(status, keyword);
        return ResponseEntity.ok(ApiResponse.success("Hubs retrieved successfully", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HubDTO>> getHub(@PathVariable UUID id) {
        HubDTO dto = getHubByIdUseCase.execute(id);
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Hub not found: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success("Hub retrieved successfully", dto));
    }

    @GetMapping("/near")
    public ResponseEntity<ApiResponse<List<HubDTO>>> listNearLocation(@RequestParam(required = false) HubStatus status, @RequestParam double lat, @RequestParam double lon, @RequestParam double radius) {
        List<HubDTO> list = hubFacade.listNearLocation(status, lat, lon, radius);
        return ResponseEntity.ok(ApiResponse.success("Hubs retrieved successfully", list));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HubDTO>> createHub(@Valid @RequestBody CreateHubRequest request) {
        HubDTO dto = hubFacade.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Hub created", dto));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HubDTO>> updateHub(@PathVariable UUID id,
                                                          @Valid @RequestBody UpdateHubRequest request) {
        HubDTO dto = hubFacade.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Hub updated", dto));
    }

    @PostMapping("/{id}/inventory/import")
    public ResponseEntity<ApiResponse<HubDTO>> stockInHubInventory(@PathVariable UUID id,
                                                                    @Valid @RequestBody StockInHubInventoryRequest request) {
        HubDTO dto = hubFacade.stockIn(id, request);
        return ResponseEntity.ok(ApiResponse.success("Hub inventory restocked", dto));
    }
}
