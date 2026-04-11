package com.drc.aidbridge.modules.user.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.user.internal.usecase.ChangePasswordUseCase;
import com.drc.aidbridge.modules.user.internal.web.dto.ChangePasswordRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserAccountController {

    private final ChangePasswordUseCase changePasswordUseCase;

    /**
     * POST /user/password/change - Đổi password cho user đã xác thực.
     */
    @PostMapping("/password/change")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        changePasswordUseCase.execute(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}