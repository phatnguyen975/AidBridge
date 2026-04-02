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

        notificationFacade.sendWelcomeEmail(user.getEmail(), user.getFullName());

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        log.info("User verified: {}", user.getEmail());
        return userMapper.buildAuthResponse(user, accessToken, refreshToken);
    }
}
