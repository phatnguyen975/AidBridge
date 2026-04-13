package com.drc.aidbridge.modules.staff.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.staff.StaffDTO;
import com.drc.aidbridge.modules.staff.internal.usecase.CreateStaffUseCase;
import com.drc.aidbridge.modules.staff.internal.usecase.GetStaffByIdUseCase;
import com.drc.aidbridge.modules.staff.internal.usecase.GetStaffByUserIdUseCase;
import com.drc.aidbridge.modules.staff.internal.web.dto.CreateStaffRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final GetStaffByIdUseCase getStaffByIdUseCase;
    private final GetStaffByUserIdUseCase getStaffByUserIdUseCase;
    private final CreateStaffUseCase createStaffUseCase;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffDTO>> getStaff(@PathVariable UUID id) {
        StaffDTO dto = getStaffByIdUseCase.execute(id);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success("Staff retrieved successfully", dto));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<ApiResponse<StaffDTO>> getByUser(@PathVariable UUID userId) {
        StaffDTO dto = getStaffByUserIdUseCase.execute(userId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success("Staff retrieved successfully", dto));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StaffDTO>> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        StaffDTO dto = createStaffUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Staff created successfully", dto));
    }
}
