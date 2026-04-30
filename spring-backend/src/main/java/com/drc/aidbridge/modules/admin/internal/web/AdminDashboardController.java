package com.drc.aidbridge.modules.admin.internal.web;

import com.drc.aidbridge.modules.admin.internal.usecase.GetAdminDashboardSummaryUseCase;
import com.drc.aidbridge.modules.admin.internal.web.dto.AdminDashboardSummaryResponse;
import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final GetAdminDashboardSummaryUseCase getAdminDashboardSummaryUseCase;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardSummaryResponse>> getSummary() {
        AdminDashboardSummaryResponse summary = getAdminDashboardSummaryUseCase.execute();
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard summary retrieved successfully", summary));
    }
}
