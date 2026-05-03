package com.drc.aidbridge.modules.staff.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.shared.exception.AuthenticationException;
import com.drc.aidbridge.modules.staff.internal.usecase.StaffOutboundInventoryUseCase;
import com.drc.aidbridge.modules.staff.internal.web.dto.ConfirmOutboundInventoryRequest;
import com.drc.aidbridge.modules.staff.internal.web.dto.InventoryQrPreviewResponse;
import com.drc.aidbridge.modules.staff.internal.web.dto.InventoryTransactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/staff/inventory")
@RequiredArgsConstructor
public class StaffInventoryTransactionController {

    private final StaffOutboundInventoryUseCase outboundInventoryUseCase;

    @GetMapping("/outbound/preview")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<InventoryQrPreviewResponse>> previewOutbound(
            @RequestParam String code,
            Authentication authentication
    ) {
        InventoryQrPreviewResponse response = outboundInventoryUseCase.previewOutbound(
                code,
                resolveCurrentUserId(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success("Outbound preview retrieved successfully", response));
    }

    @PostMapping("/outbound/confirm")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<InventoryTransactionResponse>> confirmOutbound(
            @Valid @RequestBody ConfirmOutboundInventoryRequest request,
            Authentication authentication
    ) {
        InventoryTransactionResponse response = outboundInventoryUseCase.confirmOutbound(
                request,
                resolveCurrentUserId(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
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
