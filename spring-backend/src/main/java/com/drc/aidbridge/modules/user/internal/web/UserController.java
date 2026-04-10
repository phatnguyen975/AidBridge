package com.drc.aidbridge.modules.user.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.user.internal.usecase.*;
import com.drc.aidbridge.modules.user.internal.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final RequestOtpUseCase requestOtpUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUserUseCase logoutUserUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UpdateCurrentUserUseCase updateCurrentUserUseCase;

    // ========== AUTHENTICATION ENDPOINTS (/api/auth) =========

    /**
     * POST /api/auth/register - Đăng ký user mới
     */
    @PostMapping("/api/auth/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = registerUserUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful. Please verify your email.", response));
    }

    /**
     * POST /api/auth/login - Đăng nhập
     */
    @PostMapping("/api/auth/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = loginUserUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * POST /api/auth/refresh - Làm mới access token
     */
    @PostMapping("/api/auth/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = refreshTokenUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    /**
     * POST /api/auth/logout - Đăng xuất và revoke tokens
     */
    @PostMapping("/api/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) LogoutRequest request) {
        logoutUserUseCase.execute(authHeader, request);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    /**
     * POST /api/auth/otp/request - Yêu cầu gửi OTP
     * Dùng cho: EMAIL_VERIFY, PHONE_VERIFY, PASSWORD_RESET
     */
    @PostMapping("/api/auth/otp/request")
    public ResponseEntity<ApiResponse<Void>> requestOtp(
            @Valid @RequestBody RequestOtpRequest request) {
        requestOtpUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", null));
    }

    /**
     * POST /api/auth/password/otp/resend
     */
    @PostMapping("/api/auth/password/otp/resend")
    public ResponseEntity<ApiResponse<Void>> resendPasswordOtp(
            @RequestBody RequestOtpRequest request) {
        RequestOtpRequest otpRequest = RequestOtpRequest.builder()
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .otpType("PASSWORD_RESET")
                .build();

        requestOtpUseCase.execute(otpRequest);
        return ResponseEntity.ok(ApiResponse.success("Password reset OTP resent successfully", null));
    }

    /**
     * POST /api/auth/otp/verify - Xác thực OTP
     */
    @PostMapping("/api/auth/otp/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = verifyOtpUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", response));
    }

    /**
     * POST /api/auth/password/reset - Reset password với OTP
     */
    @PostMapping("/api/auth/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", null));
    }

    /**
     * POST /api/auth/password/change - Đổi password
     */
    @PostMapping("/api/auth/password/change")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = resolveUserId(authentication);
        changePasswordUseCase.execute(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    // ========== USER ENDPOINTS (/api/users) =========

    /**
     * GET /api/users/me - Lấy thông tin user hiện tại
     */
    @GetMapping("/api/users/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        UserResponse response = getCurrentUserUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", response));
    }

    private UUID resolveUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt jwt) {
            String sub = jwt.getClaimAsString("sub");
            if (sub != null && !sub.isBlank()) {
                return UUID.fromString(sub);
            }
        }

        if (principal instanceof UUID userId) {
            return userId;
        }

        return UUID.fromString(principal.toString());
    }

    /**
     * PATCH /api/user/me - Cập nhật thông tin profile user hiện tại
     */
    @PatchMapping("/api/user/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = resolveUserId(authentication);
        UserResponse response = updateCurrentUserUseCase.execute(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }
}
