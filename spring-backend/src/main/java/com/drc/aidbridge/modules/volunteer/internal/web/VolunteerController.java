package com.drc.aidbridge.modules.volunteer.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.volunteer.internal.usecase.*;
import com.drc.aidbridge.modules.volunteer.internal.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/volunteers")
@RequiredArgsConstructor
public class VolunteerController {

    private final GetVolunteerProfileUseCase getVolunteerProfileUseCase;
    private final UpdateVolunteerProfileUseCase updateVolunteerProfileUseCase;
    private final ToggleVolunteerStatusUseCase toggleVolunteerStatusUseCase;
    private final UpdateVolunteerLocationUseCase updateVolunteerLocationUseCase;
    private final PingVolunteerHeartbeatUseCase pingVolunteerHeartbeatUseCase;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<VolunteerProfileResponse>> getVolunteerProfile(
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        VolunteerProfileResponse response = getVolunteerProfileUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.success("Volunteer profile retrieved successfully", response));
    }

    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<VolunteerProfileResponse>> updateVolunteerProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateVolunteerProfileRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        VolunteerProfileResponse response = updateVolunteerProfileUseCase.execute(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Volunteer profile updated successfully", response));
    }

    @PostMapping("/status")
    public ResponseEntity<ApiResponse<VolunteerProfileResponse>> toggleVolunteerStatus(
            Authentication authentication,
            @Valid @RequestBody ToggleVolunteerStatusRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        VolunteerProfileResponse response = toggleVolunteerStatusUseCase.execute(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Volunteer status toggled successfully", response));
    }

    @PostMapping("/location")
    public ResponseEntity<ApiResponse<Void>> updateVolunteerLocation(
            Authentication authentication,
            @Valid @RequestBody UpdateVolunteerLocationRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        updateVolunteerLocationUseCase.execute(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Location updated successfully", null));
    }

    // Heartbeat endpoint: Update location & last active time (called every 30-60s)
    @PostMapping("/ping")
    public ResponseEntity<ApiResponse<VolunteerProfileResponse>> pingVolunteerHeartbeat(
            Authentication authentication,
            @Valid @RequestBody PingVolunteerHeartbeatRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        VolunteerProfileResponse response = pingVolunteerHeartbeatUseCase.execute(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Heartbeat ping received", response));
    }
}
