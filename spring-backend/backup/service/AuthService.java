package com.drc.aidbridge.service;

import com.drc.aidbridge.dto.request.*;
import com.drc.aidbridge.dto.response.*;
import com.drc.aidbridge.entity.User;
import com.drc.aidbridge.entity.enums.UserRole;
import com.drc.aidbridge.exception.*;
import com.drc.aidbridge.redis.OtpRedisSchema;
import com.drc.aidbridge.redis.SessionCacheRedisSchema;
import com.drc.aidbridge.repository.UserRepository;
import com.drc.aidbridge.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Authentication service for user registration, login, and token management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpRedisSchema otpRedisSchema;
    private final SessionCacheRedisSchema sessionCacheRedisSchema;
    private final EmailService emailService;

    /**
     * Register a new user.
     * Creates user with isVerified=false and sends OTP email.
     */
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        // Check for existing user
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }
        if (request.getPhone() != null &&
                userRepository.existsByPhoneNumber(request.getPhone())) {
            throw new DuplicateResourceException("Phone number already registered");
        }

        // Create user entity
        User user = User.builder()
                .fullName(request.getName())
                .email(request.getEmail())
                .phoneNumber(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.valueOf(request.getRole().toUpperCase()))
                .isVerified(false)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {} with role {}", user.getEmail(), user.getRole());

        // Generate and send OTP
        String otp = otpRedisSchema.generateOtp(
                OtpRedisSchema.OtpPurpose.REGISTRATION, request.getEmail());
        emailService.sendOtpEmail(request.getEmail(), otp);

        // Generate tokens (limited until verified)
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // Cache session
        cacheUserSession(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    /**
     * Authenticate user with email and password.
     */
    public AuthResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("Account is deactivated");
        }

        // Update FCM token if provided
        if (request.getFcmToken() != null) {
            user.setFcmToken(request.getFcmToken());
            userRepository.save(user);
        }

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // Cache session
        cacheUserSession(user);

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    /**
     * Verify OTP and activate user account.
     */
    @Transactional
    public AuthResponseDto verifyOtp(OtpVerifyRequestDto request) {
        boolean valid = otpRedisSchema.verifyOtp(
                OtpRedisSchema.OtpPurpose.REGISTRATION,
                request.getEmail(),
                request.getOtp());

        if (!valid) {
            int remaining = otpRedisSchema.getRemainingAttempts(
                    OtpRedisSchema.OtpPurpose.REGISTRATION, request.getEmail());
            throw new InvalidOtpException("Invalid OTP. " + remaining + " attempts remaining.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setVerified(true);
        userRepository.save(user);

        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());

        // Generate new tokens with full access
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        log.info("User verified: {}", user.getEmail());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    /**
     * Resend OTP to user's email.
     */
    public void resendOtp(ResendOtpRequestDto request) {
        if (!userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceNotFoundException("Email not registered");
        }

        String otp = otpRedisSchema.generateOtp(
                OtpRedisSchema.OtpPurpose.REGISTRATION, request.getEmail());
        emailService.sendOtpEmail(request.getEmail(), otp);

        log.info("OTP resent to: {}", request.getEmail());
    }

    /**
     * Refresh access token using refresh token.
     */
    public TokenResponseDto refreshToken(RefreshTokenRequestDto request) {
        Claims claims;
        try {
            claims = jwtService.validateRefreshToken(request.getRefreshToken());
        } catch (JwtException e) {
            throw new AuthenticationException("Invalid or expired refresh token");
        }

        UUID userId = jwtService.extractUserId(claims);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new AuthenticationException("Account is deactivated");
        }

        // Revoke old refresh token
        jwtService.revokeToken(request.getRefreshToken());

        // Generate new token pair
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());

        log.debug("Token refreshed for user: {}", user.getEmail());
        return TokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    /**
     * Logout user and blacklist tokens.
     */
    public void logout(String authHeader, LogoutRequestDto request) {
        // Blacklist access token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            jwtService.revokeToken(accessToken);
        }

        // Blacklist refresh token if provided
        if (request != null && request.getRefreshToken() != null) {
            jwtService.revokeToken(request.getRefreshToken());
        }

        log.info("User logged out");
    }

    /**
     * Initiate password reset by sending OTP.
     */
    public void initiatePasswordReset(ForgotPasswordRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Email not registered"));

        String otp = otpRedisSchema.generateOtp(
                OtpRedisSchema.OtpPurpose.PASSWORD_RESET, request.getEmail());
        emailService.sendPasswordResetEmail(request.getEmail(), otp);

        log.info("Password reset initiated for: {}", request.getEmail());
    }

    /**
     * Reset password after OTP verification.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        boolean valid = otpRedisSchema.verifyOtp(
                OtpRedisSchema.OtpPurpose.PASSWORD_RESET,
                request.getEmail(),
                request.getOtp());

        if (!valid) {
            throw new InvalidOtpException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all existing tokens (security measure)
        jwtService.revokeAllUserTokens(user.getId());
        sessionCacheRedisSchema.deleteSession(user.getId().getLeastSignificantBits());

        log.info("Password reset for: {}", request.getEmail());
    }

    // ==================== HELPER METHODS ====================

    private void cacheUserSession(User user) {
        sessionCacheRedisSchema.saveSession(
                SessionCacheRedisSchema.UserSession.builder()
                        .userId(user.getId().getLeastSignificantBits())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole().name())
                        .avatarUrl(user.getAvatarUrl())
                        .fcmToken(user.getFcmToken())
                        .build());
    }

    private AuthResponseDto buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserDto.builder()
                        .id(user.getId().toString())
                        .name(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhoneNumber())
                        .role(user.getRole().name())
                        .avatarUrl(user.getAvatarUrl())
                        .isVerified(user.isVerified())
                        .build())
                .build();
    }
}
