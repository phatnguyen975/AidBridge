package com.drc.aidbridge.modules.mission.internal.web;

import com.drc.aidbridge.dto.response.ApiResponse;
import com.drc.aidbridge.entity.enums.MissionStatus;
import com.drc.aidbridge.entity.enums.MissionType;
import com.drc.aidbridge.modules.mission.internal.usecase.*;
import com.drc.aidbridge.modules.mission.internal.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final ListMissionsUseCase listMissionsUseCase;
    private final GetMissionUseCase getMissionUseCase;
    private final ConfirmPickupUseCase confirmPickupUseCase;
    private final CompleteMissionUseCase completeMissionUseCase;
    private final CancelMissionUseCase cancelMissionUseCase;
    private final GetTrackingUseCase getTrackingUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<MissionListResponse>> listMissions(
            @RequestParam(required = false) MissionType missionType,
            @RequestParam(required = false) MissionStatus status,
            @RequestParam(required = false) UUID volunteerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int limit) {
        MissionListResponse response = listMissionsUseCase.execute(missionType, status, volunteerId, page, limit);
        return ResponseEntity.ok(ApiResponse.success("Missions retrieved successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MissionResponse>> getMission(@PathVariable UUID id) {
        MissionResponse response = getMissionUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success("Mission retrieved successfully", response));
    }

    @PostMapping("/{id}/pickup")
    public ResponseEntity<ApiResponse<MissionResponse>> confirmPickup(
            @PathVariable UUID id,
            @RequestBody(required = false) ConfirmPickupRequest request) {
        if (request == null) {
            request = new ConfirmPickupRequest();
        }
        MissionResponse response = confirmPickupUseCase.execute(id, request);
        return ResponseEntity.ok(ApiResponse.success("Pickup confirmed successfully", response));
    }

    @PostMapping(value = "/{id}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MissionResponse>> completeMission(
            @PathVariable UUID id,
            @RequestPart(value = "confirmation_image", required = true) MultipartFile confirmationImage,
            @RequestPart(value = "notes", required = false) String notes) {
        CompleteMissionRequest request = CompleteMissionRequest.builder().notes(notes).build();
        MissionResponse response = completeMissionUseCase.execute(id, confirmationImage, request);
        return ResponseEntity.ok(ApiResponse.success("Mission completed successfully", response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<MissionResponse>> cancelMission(
            @PathVariable UUID id,
            @Valid @RequestBody CancelMissionRequest request) {
        MissionResponse response = cancelMissionUseCase.execute(id, request);
        return ResponseEntity.ok(ApiResponse.success("Mission cancelled", response));
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<ApiResponse<MissionTrackingResponse>> getTracking(@PathVariable UUID id) {
        MissionTrackingResponse response = getTrackingUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success("Tracking info retrieved", response));
    }
}
