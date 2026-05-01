package com.drc.aidbridge.modules.staff.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.shared.exception.AuthenticationException;
import com.drc.aidbridge.modules.staff.internal.service.StaffInboundInventoryService;
import com.drc.aidbridge.modules.staff.internal.web.dto.ConfirmInboundInventoryRequest;
import com.drc.aidbridge.modules.staff.internal.web.dto.ConfirmInboundInventoryResponse;
import com.drc.aidbridge.modules.staff.internal.web.dto.CreateInboundSubCategoryRequest;
import com.drc.aidbridge.modules.staff.internal.web.dto.CreateInboundSubCategoryResponse;
import com.drc.aidbridge.modules.staff.internal.web.dto.InboundDonationPreviewResponse;
import com.drc.aidbridge.modules.staff.internal.web.dto.SearchInboundSubCategoriesResponse;
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
@RequestMapping("/api/staff/inventory/inbound")
@RequiredArgsConstructor
public class StaffInboundInventoryController {

    private final StaffInboundInventoryService inboundInventoryService;

    @GetMapping("/preview")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<InboundDonationPreviewResponse>> previewInbound(
            @RequestParam String code,
            Authentication authentication
    ) {
        InboundDonationPreviewResponse response = inboundInventoryService.previewInbound(
                code,
                resolveCurrentUserId(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success("Inbound preview retrieved successfully", response));
    }

    @GetMapping("/sub-categories/search")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<SearchInboundSubCategoriesResponse>> searchSubCategories(
            @RequestParam UUID donationId,
            @RequestParam UUID parentCategoryId,
            @RequestParam(required = false) String keyword,
            Authentication authentication
    ) {
        SearchInboundSubCategoriesResponse response = inboundInventoryService.searchSubCategories(
                donationId,
                parentCategoryId,
                keyword,
                resolveCurrentUserId(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success("Sub categories retrieved successfully", response));
    }

    @PostMapping("/sub-categories")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<CreateInboundSubCategoryResponse>> createSubCategory(
            @Valid @RequestBody CreateInboundSubCategoryRequest request,
            Authentication authentication
    ) {
        CreateInboundSubCategoryResponse response = inboundInventoryService.createSubCategoryForInbound(
                request,
                resolveCurrentUserId(authentication)
        );
        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<ConfirmInboundInventoryResponse>> confirmInbound(
            @Valid @RequestBody ConfirmInboundInventoryRequest request,
            Authentication authentication
    ) {
        ConfirmInboundInventoryResponse response = inboundInventoryService.confirmInbound(
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
