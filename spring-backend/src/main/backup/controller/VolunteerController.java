package com.drc.aidbridge.controller;

import com.drc.aidbridge.dto.request.UpdateVolunteerProfileRequestDto;
import com.drc.aidbridge.dto.request.ToggleVolunteerStatusRequestDto;
import com.drc.aidbridge.dto.request.UpdateVolunteerLocationRequestDto;
import com.drc.aidbridge.dto.response.ApiResponse;
import com.drc.aidbridge.dto.response.VolunteerProfileResponseDto;
import com.drc.aidbridge.service.VolunteerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for volunteer operations.
 *
 * All endpoints require authentication (BearerAuth).
 */
@RestController
@RequestMapping("/api/volunteers")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;

    /**
     * Get volunteer profile for authenticated user.
     *
     * @param authentication Spring Security authentication object
     * @return Volunteer profile with user data
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<VolunteerProfileResponseDto>> getVolunteerProfile(
            Authentication authentication) {

        // Extract user ID from JWT token
        UUID userId = UUID.fromString(authentication.getName());

        VolunteerProfileResponseDto response = volunteerService.getVolunteerProfile(userId);

        return ResponseEntity.ok(
                ApiResponse.success("Volunteer profile retrieved successfully", response));
    }

    /**
     * Update volunteer profile.
     *
     * @param authentication Spring Security authentication object
     * @param request        Update request containing new vehicle type
     * @return Updated volunteer profile
     */
    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<VolunteerProfileResponseDto>> updateVolunteerProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateVolunteerProfileRequestDto request) {

        // Extract user ID from JWT token
        UUID userId = UUID.fromString(authentication.getName());

        VolunteerProfileResponseDto response = volunteerService.updateVolunteerProfile(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Volunteer profile updated successfully", response));
    }

    /**
     * Toggle volunteer online status and update location.
     *
     * @param authentication Spring Security authentication object
     * @param request        Status toggle request containing is_online and optional coordinates
     * @return Updated volunteer profile response
     */
    @PostMapping("/status")
    public ResponseEntity<ApiResponse<VolunteerProfileResponseDto>> toggleVolunteerStatus(
            Authentication authentication,
            @Valid @RequestBody ToggleVolunteerStatusRequestDto request) {

        // Extract user ID from JWT token
        UUID userId = UUID.fromString(authentication.getName());

        VolunteerProfileResponseDto response = volunteerService.toggleVolunteerStatus(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Volunteer status toggled successfully", response));
    }

    /**
     * Update volunteer current location.
     *
     * @param authentication Spring Security authentication object
     * @param request        Location update request containing current_lat and current_lng
     * @return Success response
     */
    @PostMapping("/location")
    public ResponseEntity<ApiResponse<Void>> updateVolunteerLocation(
            Authentication authentication,
            @Valid @RequestBody UpdateVolunteerLocationRequestDto request) {

        // Extract user ID from JWT token
        UUID userId = UUID.fromString(authentication.getName());

        volunteerService.updateVolunteerLocation(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Location updated successfully", null));
    }
}
