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
    public ResponseEntity<ApiResponse<List<HubDTO>>> listHubs(@RequestParam(required = false) HubStatus status) {
        List<HubDTO> list = hubFacade.list(status);
        return ResponseEntity.ok(ApiResponse.success("Hubs retrieved successfully", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HubDTO>> getHub(@PathVariable UUID id) {
        HubDTO dto = getHubByIdUseCase.execute(id);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success("Hub retrieved successfully", dto));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HubDTO>> createHub(@Valid @RequestBody CreateHubRequest request) {
        HubDTO dto = hubFacade.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Hub created", dto));
    }

    @PatchMapping("/{id}")
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
