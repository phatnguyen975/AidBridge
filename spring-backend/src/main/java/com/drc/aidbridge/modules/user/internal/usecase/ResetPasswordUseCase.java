package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.exception.InvalidOtpException;
import com.drc.aidbridge.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.ResetPasswordRequest;
import com.drc.aidbridge.redis.OtpRedisSchema;
import com.drc.aidbridge.redis.SessionCacheRedisSchema;
import com.drc.aidbridge.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResetPasswordUseCase {

    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpRedisSchema otpRedisSchema;
    private final JwtService jwtService;
    private final SessionCacheRedisSchema sessionCacheRedisSchema;

    @Transactional
    public void execute(ResetPasswordRequest request) {
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

        // Revoke all tokens as security measure
        jwtService.revokeAllUserTokens(user.getId());
        sessionCacheRedisSchema.deleteSession(user.getId().getLeastSignificantBits());

        log.info("Password reset for: {}", request.getEmail());
    }
}
