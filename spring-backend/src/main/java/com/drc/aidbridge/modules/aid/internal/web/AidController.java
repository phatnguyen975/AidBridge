package com.drc.aidbridge.modules.aid.internal.web;
import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.shared.dto.PaginatedResponseDto;
import com.drc.aidbridge.modules.aid.internal.usecase.*;
import com.drc.aidbridge.modules.aid.internal.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/aid-requests")
@RequiredArgsConstructor
public class AidController {

    private final CreateAidRequestUseCase createAidRequestUseCase;
    private final GetAidRequestUseCase getAidRequestUseCase;
    private final CancelAidRequestUseCase cancelAidRequestUseCase;
    private final ListAidRequestsUseCase listAidRequestsUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<AidRequestResponse>> createAidRequest(
            @Valid @RequestBody CreateAidRequest request,
            @AuthenticationPrincipal UUID userId) {
        AidRequestResponse response = createAidRequestUseCase.execute(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Aid request created", response));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AidRequestResponse>> getAidRequest(@PathVariable UUID id) {
        AidRequestResponse response = getAidRequestUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<AidRequestResponse>> cancelAidRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId,
            @RequestBody(required = false) CancelAidRequest request) {
        if (request == null) {
            request = new CancelAidRequest();
        }
        AidRequestResponse response = cancelAidRequestUseCase.execute(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Aid request cancelled", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponseDto<AidRequestResponse>>> listAidRequests(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        PaginatedResponseDto<AidRequestResponse> response = listAidRequestsUseCase.execute(page, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
