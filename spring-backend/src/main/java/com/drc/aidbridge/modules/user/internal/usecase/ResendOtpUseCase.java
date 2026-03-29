package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.ResendOtpRequest;
import com.drc.aidbridge.redis.OtpRedisSchema;
import com.drc.aidbridge.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResendOtpUseCase {

    private final UserJpaRepository userRepository;
    private final OtpRedisSchema otpRedisSchema;
    private final EmailService emailService;

    public void execute(ResendOtpRequest request) {
        if (!userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceNotFoundException("Email not registered");
        }

        String otp = otpRedisSchema.generateOtp(
                OtpRedisSchema.OtpPurpose.REGISTRATION, request.getEmail());
        emailService.sendOtpEmail(request.getEmail(), otp);

        log.info("OTP resent to: {}", request.getEmail());
    }
}
