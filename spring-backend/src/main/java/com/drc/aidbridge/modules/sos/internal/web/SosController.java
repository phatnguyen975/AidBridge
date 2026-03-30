package com.drc.aidbridge.modules.sos.internal.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.sos.internal.usecase.CreateSosRequestUseCase;
import com.drc.aidbridge.modules.sos.internal.usecase.GetSosRequestUseCase;
import com.drc.aidbridge.modules.sos.internal.usecase.ListSosRequestsUseCase;
import com.drc.aidbridge.modules.sos.internal.web.dto.CreateSosRequest;
import com.drc.aidbridge.modules.sos.internal.web.dto.SosRequestResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/victim/sos-requests")
@RequiredArgsConstructor
public class SosController {

    private final CreateSosRequestUseCase createSosRequestUseCase;
    private final GetSosRequestUseCase getSosRequestUseCase;
    private final ListSosRequestsUseCase listSosRequestsUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<SosRequestResponse>> createSosRequest(
            @Valid @RequestBody CreateSosRequest request,
            @AuthenticationPrincipal UUID userId) {

        System.out.println("UserId: " + userId); // 👈 đặt ở đây

        SosRequestResponse response = createSosRequestUseCase.execute(userId, request);
        return ResponseEntity.ok(ApiResponse.success("SOS request created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SosRequestResponse>> getSosRequest(@PathVariable UUID id) {
        SosRequestResponse response = getSosRequestUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SosRequestResponse>>> listSosRequests() {
        List<SosRequestResponse> response = listSosRequestsUseCase.execute();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
