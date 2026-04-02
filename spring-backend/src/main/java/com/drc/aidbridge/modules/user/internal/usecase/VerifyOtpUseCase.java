package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.BadRequestException;
import com.drc.aidbridge.modules.shared.exception.InvalidOtpException;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.cache.OtpRedisSchema;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.mapper.UserMapper;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.security.JwtService;
import com.drc.aidbridge.modules.user.internal.web.dto.AuthResponse;
import com.drc.aidbridge.modules.user.internal.web.dto.VerifyOtpRequest;
import com.drc.aidbridge.modules.notification.NotificationFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerifyOtpUseCase {

    private final UserJpaRepository userRepository;
    private final OtpRedisSchema otpRedisSchema;
    private final JwtService jwtService;
    private final NotificationFacade notificationFacade;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse execute(VerifyOtpRequest request) {
        validateRequest(request);

        String identifier = getIdentifier(request);
        OtpRedisSchema.OtpPurpose purpose = mapOtpType(request.getOtpType());

        boolean valid = otpRedisSchema.verifyOtp(purpose, identifier, request.getOtpCode());

        if (!valid) {
            int remaining = otpRedisSchema.getRemainingAttempts(purpose, identifier);
            if (remaining <= 0) {
                throw new InvalidOtpException(
                        "Account locked due to too many failed attempts. Please request a new OTP.");
            }
            throw new InvalidOtpException("Invalid OTP. " + remaining + " attempts remaining.");
        }

        User user = findUser(request);

        // Handle based on OTP type
        switch (request.getOtpType()) {
            case "EMAIL_VERIFY", "PHONE_VERIFY" -> {
                user.setVerified(true);
                userRepository.save(user);
                notificationFacade.sendWelcomeEmail(user.getEmail(), user.getFullName());
                log.info("User verified: {}", user.getEmail());
            }
            case "PASSWORD_RESET" -> {
                // For password reset, just verify OTP is valid
                // Actual password change happens in /password/reset endpoint
                log.info("Password reset OTP verified for: {}", identifier);
            }
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        return userMapper.buildAuthResponse(user, accessToken, refreshToken);
    }

    private void validateRequest(VerifyOtpRequest request) {
        if (!StringUtils.hasText(request.getEmail()) && !StringUtils.hasText(request.getPhoneNumber())) {
            throw new BadRequestException("Either email or phone_number is required");
        }
    }

    private String getIdentifier(VerifyOtpRequest request) {
        if (StringUtils.hasText(request.getEmail())) {
            return request.getEmail();
        }
        return request.getPhoneNumber();
    }

    private OtpRedisSchema.OtpPurpose mapOtpType(String otpType) {
        return switch (otpType) {
            case "EMAIL_VERIFY", "PHONE_VERIFY" -> OtpRedisSchema.OtpPurpose.REGISTRATION;
            case "PASSWORD_RESET" -> OtpRedisSchema.OtpPurpose.PASSWORD_RESET;
            default -> throw new BadRequestException("Invalid OTP type: " + otpType);
        };
    }

    private User findUser(VerifyOtpRequest request) {
        if (StringUtils.hasText(request.getEmail())) {
            return userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
        return userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
