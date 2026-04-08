package com.drc.aidbridge.modules.hub.internal.web;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.internal.usecase.GetHubByIdUseCase;
import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hubs")
@RequiredArgsConstructor
public class HubController {

    private final GetHubByIdUseCase getHubByIdUseCase;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HubDTO>> getHub(@PathVariable UUID id) {
        HubDTO dto = getHubByIdUseCase.execute(id);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success("Hub retrieved successfully", dto));
    }
}
