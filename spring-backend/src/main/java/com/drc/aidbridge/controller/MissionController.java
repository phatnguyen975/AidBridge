package com.drc.aidbridge.controller;

import com.drc.aidbridge.dto.request.CancelMissionRequestDto;
import com.drc.aidbridge.dto.request.CompleteMissionRequestDto;
import com.drc.aidbridge.dto.request.ConfirmPickupRequestDto;
import com.drc.aidbridge.dto.response.ApiResponse;
import com.drc.aidbridge.dto.response.MissionListResponseDto;
import com.drc.aidbridge.dto.response.MissionResponseDto;
import com.drc.aidbridge.dto.response.MissionTrackingResponseDto;
import com.drc.aidbridge.entity.enums.MissionStatus;
import com.drc.aidbridge.entity.enums.MissionType;
import com.drc.aidbridge.service.MissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Mission API Controller.
 *
 * Endpoints:
 * - GET /api/missions - List missions with filters
 * - GET /api/missions/{id} - Get mission details (cached for active missions)
 * - POST /api/missions/{id}/pickup - Confirm pickup at hub
 * - POST /api/missions/{id}/complete - Complete mission
 * - POST /api/missions/{id}/cancel - Cancel mission
 * - GET /api/missions/{id}/tracking - Get real-time tracking info
 *
 * Cache Strategy:
 * - GET /missions: Not cached (list changes frequently)
 * - GET /missions/{id}: Redis cache, TTL 5 min for active missions
 * - GET /missions/{id}/tracking: Redis geo data for volunteer location
 */
@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    /**
     * GET /api/missions
     * List missions with optional filters and pagination.
     *
     * Cache: Not cached - data changes frequently.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<MissionListResponseDto>> listMissions(
            @RequestParam(required = false) MissionType missionType,
            @RequestParam(required = false) MissionStatus status,
            @RequestParam(required = false) UUID volunteerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int limit) {

        MissionListResponseDto response = missionService.listMissions(
                missionType, status, volunteerId, page, limit);

        return ResponseEntity.ok(ApiResponse.success("Missions retrieved successfully", response));
    }

    /**
     * GET /api/missions/{id}
     * Get mission details by ID.
     *
     * Cache: Redis with TTL 5 minutes for active missions.
     * Active statuses: ASSIGNED, PICKING_UP, PICKED_UP, IN_TRANSIT, DISPATCHING
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MissionResponseDto>> getMission(@PathVariable UUID id) {
        MissionResponseDto response = missionService.getMission(id);
        return ResponseEntity.ok(ApiResponse.success("Mission retrieved successfully", response));
    }

    /**
     * POST /api/missions/{id}/pickup
     * Confirm pickup at hub (for DELIVERY missions).
     *
     * Cache: Invalidates mission:{id} cache after successful pickup.
     * Triggers: Supabase Realtime status update + FCM notification.
     */
    @PostMapping("/{id}/pickup")
    public ResponseEntity<ApiResponse<MissionResponseDto>> confirmPickup(
            @PathVariable UUID id,
            @RequestBody(required = false) ConfirmPickupRequestDto request) {

        if (request == null) {
            request = new ConfirmPickupRequestDto();
        }

        MissionResponseDto response = missionService.confirmPickup(id, request);
        return ResponseEntity.ok(ApiResponse.success("Pickup confirmed successfully", response));
    }

    /**
     * POST /api/missions/{id}/complete
     * Complete mission with confirmation image.
     *
     * Cache: Removes mission from active cache.
     * Triggers: Status update, FCM notification, volunteer stats update.
     */
    @PostMapping(value = "/{id}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MissionResponseDto>> completeMission(
            @PathVariable UUID id,
            @RequestPart(value = "confirmation_image", required = true) MultipartFile confirmationImage,
            @RequestPart(value = "notes", required = false) String notes) {

        CompleteMissionRequestDto request = CompleteMissionRequestDto.builder()
                .notes(notes)
                .build();

        MissionResponseDto response = missionService.completeMission(id, confirmationImage, request);
        return ResponseEntity.ok(ApiResponse.success("Mission completed successfully", response));
    }

    /**
     * POST /api/missions/{id}/cancel
     * Cancel an active mission.
     *
     * Cache: Removes mission from active cache.
     * Triggers: Status update, FCM notification, re-dispatch if needed.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<MissionResponseDto>> cancelMission(
            @PathVariable UUID id,
            @Valid @RequestBody CancelMissionRequestDto request) {

        MissionResponseDto response = missionService.cancelMission(id, request);
        return ResponseEntity.ok(ApiResponse.success("Mission cancelled", response));
    }

    /**
     * GET /api/missions/{id}/tracking
     * Get real-time tracking information for an active mission.
     *
     * Cache: Volunteer location from Redis geo data (volunteer:locations).
     * Real-time: Subscribe to Supabase Realtime channel for live updates.
     */
    @GetMapping("/{id}/tracking")
    public ResponseEntity<ApiResponse<MissionTrackingResponseDto>> getTracking(@PathVariable UUID id) {
        MissionTrackingResponseDto response = missionService.getTracking(id);
        return ResponseEntity.ok(ApiResponse.success("Tracking info retrieved", response));
    }
}
