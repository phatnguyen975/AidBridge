package com.drc.aidbridge.modules.volunteer.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.volunteer.internal.usecase.*;
import com.drc.aidbridge.modules.volunteer.internal.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.drc.aidbridge.modules.mission.DispatchAttemptDTO;
import java.math.BigDecimal;
import java.util.UUID;
import com.drc.aidbridge.modules.volunteer.VolunteerDTO;
import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.mission.MissionHistoryFullDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.List;

import com.drc.aidbridge.modules.mission.internal.web.dto.CancelMissionRequest;
import com.drc.aidbridge.modules.mission.internal.web.dto.CompleteMissionRequest;
import com.drc.aidbridge.modules.mission.MissionDTO;

@RestController
@RequestMapping("/api/volunteers")
@RequiredArgsConstructor
public class VolunteerController {

    private final GetVolunteerProfileUseCase getVolunteerProfileUseCase;
    private final UpdateVolunteerProfileUseCase updateVolunteerProfileUseCase;
    private final ToggleVolunteerStatusUseCase toggleVolunteerStatusUseCase;
    private final PingVolunteerHeartbeatUseCase pingVolunteerHeartbeatUseCase;
    // private final GetVolunteerStatisticsUseCase getVolunteerStatisticsUseCase;
    private final GetVolunteerMissionHistoryUseCase getVolunteerMissionHistoryUseCase;
    private final FindNearbyVolunteersUseCase findNearbyVolunteersUseCase;
    private final GetLatestVolunteerDispatchUseCase getLatestVolunteerDispatchUseCase;
    private final CancelDispatchAttemptUseCase cancelDispatchAttemptUseCase;
    private final AcceptDispatchAttemptUseCase acceptDispatchAttemptUseCase;
    private final GetVolunteerMissionHistoryFullUseCase getVolunteerMissionHistoryFullUseCase;
    private final GetVolunteerCurrentMissionUseCase getVolunteerCurrentMissionUseCase;
    private final MissionFacade missionFacade;

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

    // Heartbeat endpoint: Update location & last active time (called every 30-60s)
    @PostMapping("/ping")
    public ResponseEntity<ApiResponse<VolunteerProfileResponse>> pingVolunteerHeartbeat(
            Authentication authentication,
            @Valid @RequestBody PingVolunteerHeartbeatRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        VolunteerProfileResponse response = pingVolunteerHeartbeatUseCase.execute(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Heartbeat ping received", response));
    }

    // @GetMapping("/{volunteerId}/statistics")
    // public ResponseEntity<ApiResponse<VolunteerStatisticsResponse>>
    // getVolunteerStatistics(
    // @PathVariable UUID volunteerId) {
    // VolunteerStatisticsResponse response =
    // getVolunteerStatisticsUseCase.execute(volunteerId);
    // return ResponseEntity.ok(ApiResponse.success("Volunteer statistics retrieved
    // successfully", response));
    // }

    @GetMapping("/missions/history")
    public ResponseEntity<ApiResponse<VolunteerMissionHistoryResponse>> getVolunteerMissionHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        UUID userId = UUID.fromString(authentication.getName());
        VolunteerMissionHistoryResponse response = getVolunteerMissionHistoryUseCase.execute(userId, page, limit);
        return ResponseEntity.ok(ApiResponse.success("Volunteer mission history retrieved successfully", response));
    }

    @GetMapping("/missions/history/full")
    public ResponseEntity<ApiResponse<VolunteerMissionHistoryFullResponse>> getVolunteerMissionHistoryFull(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        UUID userId = UUID.fromString(authentication.getName());
        VolunteerMissionHistoryFullResponse response = getVolunteerMissionHistoryFullUseCase.execute(userId, page, limit);
        return ResponseEntity.ok(ApiResponse.success("Volunteer full mission history retrieved successfully", response));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<VolunteerDTO>>> getVolunteerNearby(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng) {
        List<VolunteerDTO> response = findNearbyVolunteersUseCase.execute(lat, lng);
        return ResponseEntity.ok(ApiResponse.success("Nearby volunteers retrieved successfully", response));
    }

    @GetMapping("/missions/dispatch/latest")
    public ResponseEntity<ApiResponse<DispatchAttemptDTO>> getLatestDispatchAttempt(
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        DispatchAttemptDTO attempt = getLatestVolunteerDispatchUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.success("Latest dispatch attempt retrieved successfully", attempt));
    }

    @PatchMapping("/missions/dispatch/cancel")
    public ResponseEntity<ApiResponse<DispatchAttemptDTO>> cancelDispatchAttempt(
            Authentication authentication,
            @Valid @RequestBody CancelDispatchAttemptRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        DispatchAttemptDTO attempt = cancelDispatchAttemptUseCase.execute(userId, request.getDispatchAttemptId());
        return ResponseEntity.ok(ApiResponse.success("Dispatch attempt cancelled successfully", attempt));
    }

    @PatchMapping("/missions/dispatch/accept")
    public ResponseEntity<ApiResponse<DispatchAttemptDTO>> acceptDispatchAttempt(
            Authentication authentication,
            @Valid @RequestBody AcceptDispatchAttemptRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        DispatchAttemptDTO attempt = acceptDispatchAttemptUseCase.execute(userId, request.getDispatchAttemptId());
        return ResponseEntity.ok(ApiResponse.success("Dispatch attempt accepted successfully", attempt));
    }

    // Current mission of current volunteer
    @GetMapping("/missions/current")
    public ResponseEntity<ApiResponse<MissionHistoryFullDTO>> getCurrentMission(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        MissionHistoryFullDTO response = getVolunteerCurrentMissionUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.success("Current mission retrieved successfully", response));
    }

    @PostMapping("/missions/complete")
    public ResponseEntity<ApiResponse<MissionDTO>> completeMission(
            @Valid @RequestBody CompleteMissionRequest request) {
        MissionDTO response = missionFacade.completeMission(request.getMissionId(), request.getNotes());
        return ResponseEntity.ok(ApiResponse.success("Mission completed successfully", response));
    }

    @PostMapping("/missions/cancel")
    public ResponseEntity<ApiResponse<MissionDTO>> cancelMission(
            @Valid @RequestBody CancelMissionRequest request) {
        MissionDTO response = missionFacade.cancelMission(request.getMissionId(), request.getCancellationReason());
        return ResponseEntity.ok(ApiResponse.success("Mission cancelled successfully", response));
    }
}
