package com.drc.aidbridge.controller;

import com.drc.aidbridge.dto.request.CreateAidRequestDto;
import com.drc.aidbridge.dto.response.AidRequestResponseDto;
import com.drc.aidbridge.dto.response.ApiResponse;
import com.drc.aidbridge.service.AidRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/victim/aid-requests")
@RequiredArgsConstructor
public class AidRequestController {

    private final AidRequestService aidRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<AidRequestResponseDto>> createAidRequest(
            @Valid @RequestBody CreateAidRequestDto request,
            @AuthenticationPrincipal UUID userId) {

        AidRequestResponseDto response = aidRequestService.createAidRequest(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Aid request created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AidRequestResponseDto>> getAidRequest(@PathVariable UUID id) {
        AidRequestResponseDto response = aidRequestService.getAidRequest(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<AidRequestResponseDto>>> listAidRequests() {
        java.util.List<AidRequestResponseDto> response = aidRequestService.listAidRequests();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

