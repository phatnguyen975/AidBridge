package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.cache.OtpRedisSchema;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.ResendOtpRequest;
import com.drc.aidbridge.modules.notification.NotificationFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResendOtpUseCase {

    private final UserJpaRepository userRepository;
    private final OtpRedisSchema otpRedisSchema;
    private final NotificationFacade notificationFacade;

    public void execute(ResendOtpRequest request) {
        if (!userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceNotFoundException("Email not registered");
        }

        OtpRedisSchema.OtpPurpose purpose = resolvePurpose(request.getEmail());

        String otp = otpRedisSchema.generateOtp(
                purpose, request.getEmail());

        if (purpose == OtpRedisSchema.OtpPurpose.PASSWORD_RESET) {
            notificationFacade.sendPasswordResetEmail(request.getEmail(), otp);
        } else {
            notificationFacade.sendEmail(request.getEmail(), otp);
        }

        log.info("OTP resent to: {} with purpose {}", request.getEmail(), purpose.getValue());
    }

    private OtpRedisSchema.OtpPurpose resolvePurpose(String email) {
        if (otpRedisSchema.hasOtp(OtpRedisSchema.OtpPurpose.PASSWORD_RESET, email)) {
            return OtpRedisSchema.OtpPurpose.PASSWORD_RESET;
        }
        return OtpRedisSchema.OtpPurpose.REGISTRATION;
    }
}
