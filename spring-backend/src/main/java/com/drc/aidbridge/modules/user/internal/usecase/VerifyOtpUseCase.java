package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.InvalidOtpException;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.cache.OtpRedisSchema;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.mapper.UserMapper;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.AuthResponse;
import com.drc.aidbridge.modules.user.internal.web.dto.OtpVerifyRequest;
import com.drc.aidbridge.infrastructure.security.JwtService;
import com.drc.aidbridge.modules.notification.NotificationFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    public AuthResponse execute(OtpVerifyRequest request) {
        OtpRedisSchema.OtpPurpose purpose = resolvePurpose(request.getEmail());

        boolean valid;
        if (purpose == OtpRedisSchema.OtpPurpose.PASSWORD_RESET) {
            valid = otpRedisSchema.verifyOtpWithoutConsuming(
                    purpose,
                    request.getEmail(),
                    request.getOtp());
        } else {
            valid = otpRedisSchema.verifyOtp(
                    purpose,
                    request.getEmail(),
                    request.getOtp());
        }

        if (!valid) {
            int remaining = otpRedisSchema.getRemainingAttempts(
                    purpose, request.getEmail());
            throw new InvalidOtpException("Invalid OTP. " + remaining + " attempts remaining.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (purpose == OtpRedisSchema.OtpPurpose.REGISTRATION) {
            user.setVerified(true);
            userRepository.save(user);
            notificationFacade.sendWelcomeEmail(user.getEmail(), user.getFullName());
        }

        // For password reset OTP verification we keep contract compatibility by returning
        // AuthResponse payload, but caller may ignore these tokens.
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        log.info("OTP verified for {} with purpose {}", user.getEmail(), purpose.getValue());
        return userMapper.buildAuthResponse(user, accessToken, refreshToken);
    }

    private OtpRedisSchema.OtpPurpose resolvePurpose(String email) {
        if (otpRedisSchema.hasOtp(OtpRedisSchema.OtpPurpose.PASSWORD_RESET, email)) {
            return OtpRedisSchema.OtpPurpose.PASSWORD_RESET;
        }
        return OtpRedisSchema.OtpPurpose.REGISTRATION;
    }
}
