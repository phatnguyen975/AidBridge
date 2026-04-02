package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.BadRequestException;
import com.drc.aidbridge.modules.shared.exception.InvalidOtpException;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.cache.OtpRedisSchema;
import com.drc.aidbridge.modules.user.internal.cache.SessionCacheRedisSchema;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.security.JwtService;
import com.drc.aidbridge.modules.user.internal.web.dto.ResetPasswordRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
        // Validate: email OR phone required
        if (!StringUtils.hasText(request.getEmail()) && !StringUtils.hasText(request.getPhoneNumber())) {
            throw new BadRequestException("Either email or phone_number is required");
        }

        String identifier = StringUtils.hasText(request.getEmail())
                ? request.getEmail()
                : request.getPhoneNumber();

        boolean valid = otpRedisSchema.verifyOtp(
                OtpRedisSchema.OtpPurpose.PASSWORD_RESET,
                identifier,
                request.getOtpCode());

        if (!valid) {
            throw new InvalidOtpException("Invalid or expired OTP");
        }

        User user = findUser(request);

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all tokens as security measure
        jwtService.revokeAllUserTokens(user.getId());
        sessionCacheRedisSchema.deleteSession(user.getId().getLeastSignificantBits());

        log.info("Password reset for: {}", identifier);
    }

    private User findUser(ResetPasswordRequest request) {
        if (StringUtils.hasText(request.getEmail())) {
            return userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
        return userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
