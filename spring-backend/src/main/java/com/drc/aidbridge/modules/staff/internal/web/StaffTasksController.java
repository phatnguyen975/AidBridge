package com.drc.aidbridge.modules.staff.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.shared.exception.AuthenticationException;
import com.drc.aidbridge.modules.staff.internal.service.StaffTasksService;
import com.drc.aidbridge.modules.staff.internal.web.dto.StaffUpcomingDeliveryMissionResponse;
import com.drc.aidbridge.modules.staff.internal.web.dto.StaffUpcomingDonationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/staff/tasks")
@RequiredArgsConstructor
public class StaffTasksController {

    private final StaffTasksService staffTasksService;

    @GetMapping("/upcoming/donations")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<List<StaffUpcomingDonationResponse>>> getUpcomingDonations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        List<StaffUpcomingDonationResponse> response = staffTasksService.getUpcomingDonations(currentUserId, page, limit);
        return ResponseEntity.ok(ApiResponse.success("Upcoming donations retrieved successfully", response));
    }

    @GetMapping("/upcoming/deliveries")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<List<StaffUpcomingDeliveryMissionResponse>>> getUpcomingDeliveries(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        List<StaffUpcomingDeliveryMissionResponse> response = staffTasksService.getUpcomingDeliveryMissions(currentUserId, page, limit);
        return ResponseEntity.ok(ApiResponse.success("Upcoming delivery missions retrieved successfully", response));
    }

    private UUID resolveCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AuthenticationException("Unauthorized request");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new AuthenticationException("Invalid authentication context");
        }
    }
}
