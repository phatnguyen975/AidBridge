package com.drc.aidbridge.modules.staff.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.shared.exception.AuthenticationException;
import com.drc.aidbridge.modules.staff.internal.service.StaffInventoryService;
import com.drc.aidbridge.modules.staff.internal.web.dto.StaffInventoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/staff/inventory")
@RequiredArgsConstructor
public class StaffInventoryController {

    private final StaffInventoryService staffInventoryService;

    @GetMapping
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<StaffInventoryResponse>> getMyHubInventory(
            @RequestParam(required = false) UUID parentCategoryId,
            @RequestParam(required = false) String parentCategoryName,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication
    ) {
        UUID currentUserId = resolveCurrentUserId(authentication);
        StaffInventoryResponse response = staffInventoryService.getMyHubInventory(
                currentUserId,
                parentCategoryId,
                parentCategoryName,
                keyword,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success("Staff inventory retrieved successfully", response));
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
