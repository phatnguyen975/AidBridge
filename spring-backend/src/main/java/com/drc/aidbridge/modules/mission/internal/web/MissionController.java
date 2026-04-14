package com.drc.aidbridge.modules.mission.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import com.drc.aidbridge.modules.shared.exception.BadRequestException;
import com.drc.aidbridge.modules.mission.internal.usecase.*;
import com.drc.aidbridge.modules.mission.internal.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
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
    private final CreateMissionUseCase createMissionUseCase;
    private final GetMyMissionsUseCase getMyMissionsUseCase;
    private final GetActiveMissionsUseCase getActiveMissionsUseCase;
    private final GetMissionStatsUseCase getMissionStatsUseCase;
    private final AcceptMissionUseCase acceptMissionUseCase;
    private final RejectMissionUseCase rejectMissionUseCase;
    private final AssignMissionUseCase assignMissionUseCase;
    private final StartMissionUseCase startMissionUseCase;
    private final GetDispatchAttemptsUseCase getDispatchAttemptsUseCase;

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

        // Validate that the file is not empty
        if (confirmationImage == null || confirmationImage.isEmpty()) {
            throw new BadRequestException("Confirmation image is required and cannot be empty");
        }

        if (confirmationImage.getSize() == 0) {
            throw new BadRequestException("Confirmation image cannot be empty");
        }

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

    /**
     * Tạo mission mới
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<MissionResponse>> createMission(
            @Valid @RequestBody CreateMissionRequest request) {
        MissionResponse response = createMissionUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Mission created successfully", response));
    }

    /**
     * Lấy danh sách missions của volunteer đang login
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<ApiResponse<MyMissionsResponse>> getMyMissions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "true") boolean includeHistory,
            @RequestParam(defaultValue = "1") int historyPage,
            @RequestParam(defaultValue = "10") int historyLimit) {
        UUID volunteerId = UUID.fromString(jwt.getSubject());
        MyMissionsResponse response = getMyMissionsUseCase.execute(volunteerId, includeHistory, historyPage,
                historyLimit);
        return ResponseEntity.ok(ApiResponse.success("My missions retrieved successfully", response));
    }

    /**
     * Lấy danh sách missions đang active (dashboard cho Staff/Admin)
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<ActiveMissionsResponse>> getActiveMissions(
            @RequestParam(required = false) MissionType missionType,
            @RequestParam(required = false) UUID hubId,
            @RequestParam(defaultValue = "true") boolean includeStats) {
        ActiveMissionsResponse response = getActiveMissionsUseCase.execute(missionType, hubId, includeStats);
        return ResponseEntity.ok(ApiResponse.success("Active missions retrieved successfully", response));
    }

    /**
     * Lấy thống kê missions (Staff/Admin)
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<MissionStatsResponse>> getMissionStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        MissionStatsResponse response = getMissionStatsUseCase.execute(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success("Mission stats retrieved successfully", response));
    }

    /**
     * Volunteer chấp nhận dispatch request
     */
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<ApiResponse<MissionResponse>> acceptMission(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) AcceptMissionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID volunteerId = UUID.fromString(jwt.getSubject());
        if (request == null) {
            request = new AcceptMissionRequest();
        }
        MissionResponse response = acceptMissionUseCase.execute(id, volunteerId, request);
        return ResponseEntity.ok(ApiResponse.success("Mission accepted successfully", response));
    }

    /**
     * Volunteer từ chối dispatch request
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<ApiResponse<Void>> rejectMission(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) RejectMissionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID volunteerId = UUID.fromString(jwt.getSubject());
        if (request == null) {
            request = new RejectMissionRequest();
        }
        rejectMissionUseCase.execute(id, volunteerId, request);
        return ResponseEntity.ok(ApiResponse.success("Mission rejected", null));
    }

    /**
     * Staff/Admin manually assign volunteer vào mission
     */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<MissionResponse>> assignMission(
            @PathVariable UUID id,
            @Valid @RequestBody AssignMissionRequest request) {
        MissionResponse response = assignMissionUseCase.execute(id, request);
        return ResponseEntity.ok(ApiResponse.success("Mission assigned successfully", response));
    }

    /**
     * Volunteer bắt đầu thực hiện mission
     */
    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<ApiResponse<MissionResponse>> startMission(
            @PathVariable UUID id,
            @RequestBody(required = false) StartMissionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID volunteerId = UUID.fromString(jwt.getSubject());
        if (request == null) {
            request = new StartMissionRequest();
        }
        MissionResponse response = startMissionUseCase.execute(id, volunteerId, request);
        return ResponseEntity.ok(ApiResponse.success("Mission started successfully", response));
    }

    /**
     * Lấy lịch sử dispatch attempts của mission (Staff/Admin)
     */
    @GetMapping("/{id}/dispatch-attempts")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<DispatchAttemptsListResponse>> getDispatchAttempts(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        DispatchAttemptsListResponse response = getDispatchAttemptsUseCase.execute(id, pageable);
        return ResponseEntity.ok(ApiResponse.success("Dispatch attempts retrieved successfully", response));
    }
}
