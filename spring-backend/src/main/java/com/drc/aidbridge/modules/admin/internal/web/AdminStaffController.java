package com.drc.aidbridge.modules.admin.internal.web;

import com.drc.aidbridge.modules.admin.internal.usecase.CreateAdminStaffUseCase;
import com.drc.aidbridge.modules.admin.internal.usecase.ListAdminStaffUseCase;
import com.drc.aidbridge.modules.admin.internal.web.dto.AdminStaffResponse;
import com.drc.aidbridge.modules.admin.internal.web.dto.CreateAdminStaffRequest;
import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/staff")
@RequiredArgsConstructor
public class AdminStaffController {

    private final ListAdminStaffUseCase listAdminStaffUseCase;
    private final CreateAdminStaffUseCase createAdminStaffUseCase;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminStaffResponse>>> listStaff() {
        return ResponseEntity.ok(ApiResponse.success(
                "Staff retrieved successfully",
                listAdminStaffUseCase.execute()
        ));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminStaffResponse>> createStaff(
            @Valid @RequestBody CreateAdminStaffRequest request
    ) {
        AdminStaffResponse staff = createAdminStaffUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff created successfully", staff));
    }
}
