package com.drc.aidbridge.controller;

import com.drc.aidbridge.dto.request.CreateGuestSosRequestDto;
import com.drc.aidbridge.dto.response.ApiResponse;
import com.drc.aidbridge.dto.response.SosRequestResponseDto;
import com.drc.aidbridge.service.SosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sos")
@RequiredArgsConstructor
public class GuestSosController {

    private final SosService sosService;

    @PostMapping
    public ResponseEntity<ApiResponse<SosRequestResponseDto>> createGuestSosRequest(
            @Valid @RequestBody CreateGuestSosRequestDto request) {
        SosRequestResponseDto response = sosService.createGuestSosRequest(request);
        return ResponseEntity.ok(ApiResponse.success("Guest SOS request created successfully", response));
    }
}
