package com.drc.aidbridge.modules.user.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.user.internal.usecase.*;
import com.drc.aidbridge.modules.user.internal.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
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

    /**
     * POST /auth/register - Đăng ký user mới
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = registerUserUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful. Please verify your email.", response));
    }

    /**
     * POST /auth/login - Đăng nhập
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = loginUserUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * POST /auth/refresh - Làm mới access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = refreshTokenUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    /**
     * POST /auth/logout - Đăng xuất và revoke tokens
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) LogoutRequest request) {
        logoutUserUseCase.execute(authHeader, request);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    /**
     * POST /auth/otp/request - Yêu cầu gửi OTP
     * Dùng cho: EMAIL_VERIFY, PHONE_VERIFY, PASSWORD_RESET
     */
    @PostMapping("/otp/request")
    public ResponseEntity<ApiResponse<Void>> requestOtp(
            @Valid @RequestBody RequestOtpRequest request) {
        requestOtpUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", null));
    }

    /**
     * POST /auth/password/otp/resend
     */
    @PostMapping("/password/otp/resend")
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
     * POST /auth/otp/verify - Xác thực OTP
     */
    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = verifyOtpUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", response));
    }

    
    /**
     * POST /auth/password/reset - Reset password với OTP
     */
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", null));
    }

    /**
     * POST /auth/password/change - Đổi password
     */
    @PostMapping("/password/change")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = resolveUserId(authentication);
        changePasswordUseCase.execute(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    private UUID resolveUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UUID userId) {
            return userId;
        }
        return UUID.fromString(principal.toString());
    }
}
