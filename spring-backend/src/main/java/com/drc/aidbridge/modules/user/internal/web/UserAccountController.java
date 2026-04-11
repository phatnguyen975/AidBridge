package com.drc.aidbridge.modules.user.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.user.internal.usecase.ChangePasswordUseCase;
import com.drc.aidbridge.modules.user.internal.usecase.UpdateUserAvatarUseCase;
import com.drc.aidbridge.modules.user.internal.usecase.UpdateUserProfileUseCase;
import com.drc.aidbridge.modules.user.internal.web.dto.UpdateUserProfileRequest;
import com.drc.aidbridge.modules.user.internal.web.dto.UserAvatarUploadResponse;
import com.drc.aidbridge.modules.user.internal.web.dto.ChangePasswordRequest;
import com.drc.aidbridge.modules.user.internal.web.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserAccountController {

    private final ChangePasswordUseCase changePasswordUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final UpdateUserAvatarUseCase updateUserAvatarUseCase;

    /**
     * PUT /user/profile - Cập nhật thông tin hồ sơ cơ bản cho user đã xác thực.
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        UUID userId = resolveUserId(authentication);
        UserResponse response = updateUserProfileUseCase.execute(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    /**
     * POST /user/password/change - Đổi password cho user đã xác thực.
     */
    @PostMapping("/password/change")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = resolveUserId(authentication);
        changePasswordUseCase.execute(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    /**
     * POST /user/avatar - Upload và cập nhật avatar cho user đã xác thực.
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserAvatarUploadResponse>> uploadAvatar(
            Authentication authentication,
            @RequestPart("avatar") MultipartFile avatarFile) {
        UUID userId = resolveUserId(authentication);
        UserAvatarUploadResponse response = updateUserAvatarUseCase.execute(userId, avatarFile);
        return ResponseEntity.ok(ApiResponse.success("Avatar uploaded successfully", response));
    }

    private UUID resolveUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        return UUID.fromString(authentication.getName());
    }
}
