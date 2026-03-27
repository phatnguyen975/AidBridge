package com.drc.aidbridge.controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.drc.aidbridge.dto.request.CreateSosRequestDto;
import com.drc.aidbridge.dto.response.ApiResponse;
import com.drc.aidbridge.dto.response.SosRequestResponseDto;
import com.drc.aidbridge.service.SosService;
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

    private final SosService sosService;

    @PostMapping
    public ResponseEntity<ApiResponse<SosRequestResponseDto>> createSosRequest(
            @Valid @RequestBody CreateSosRequestDto request,
            @AuthenticationPrincipal UUID userId) {

        System.out.println("UserId: " + userId); // 👈 đặt ở đây

        SosRequestResponseDto response = sosService.createSosRequest(userId, request);
        return ResponseEntity.ok(ApiResponse.success("SOS request created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SosRequestResponseDto>> getSosRequest(@PathVariable UUID id) {
        SosRequestResponseDto response = sosService.getSosRequest(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SosRequestResponseDto>>> listSosRequests() {
        List<SosRequestResponseDto> response = sosService.listSosRequests();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
