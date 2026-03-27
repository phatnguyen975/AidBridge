package com.drc.aidbridge.controller;

import com.drc.aidbridge.dto.request.*;
import com.drc.aidbridge.dto.response.*;
import com.drc.aidbridge.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for authentication operations.
 *
 * All endpoints are public and accessible without authentication.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user.
     * Roles allowed: VICTIM, VOLUNTEER, SPONSOR
     * Triggers OTP email for verification.
     *
     * @param request Registration details
     * @return AuthResponse with tokens and user info
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDto>> register(
            @Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful. Please verify your email.", response));
    }

    /**
     * Authenticate user with email and password.
     *
     * @param request Login credentials
     * @return AuthResponse with tokens and user info
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Verify email OTP after registration.
     *
     * @param request Email and OTP code
     * @return AuthResponse with new tokens
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponseDto>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequestDto request) {
        AuthResponseDto response = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", response));
    }

    /**
     * Resend OTP to user's email.
     *
     * @param request Email address
     * @return Success message
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @Valid @RequestBody ResendOtpRequestDto request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", null));
    }

    /**
     * Refresh access token using refresh token.
     *
     * @param request Refresh token
     * @return New access and refresh tokens
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponseDto>> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDto request) {
        TokenResponseDto response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    /**
     * Logout and blacklist tokens.
     *
     * @param authHeader Authorization header with access token
     * @param request    Optional refresh token to blacklist
     * @return Success message
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) LogoutRequestDto request) {
        authService.logout(authHeader, request);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    /**
     * Initiate password reset flow.
     * Sends OTP to user's email.
     *
     * @param request Email address
     * @return Success message
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDto request) {
        authService.initiatePasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset OTP sent", null));
    }

    /**
     * Reset password after OTP verification.
     * Revokes all existing tokens for security.
     *
     * @param request Email, OTP, and new password
     * @return Success message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", null));
    }
}
