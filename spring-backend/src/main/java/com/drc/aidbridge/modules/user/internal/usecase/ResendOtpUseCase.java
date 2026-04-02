package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.cache.OtpRedisSchema;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.RequestOtpRequest;
import com.drc.aidbridge.modules.notification.NotificationFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Deprecated
@Slf4j
@Component
@RequiredArgsConstructor
public class ResendOtpUseCase {

    private final UserJpaRepository userRepository;
    private final OtpRedisSchema otpRedisSchema;
    private final NotificationFacade notificationFacade;

    /**
     * @deprecated Use RequestOtpUseCase.execute(RequestOtpRequest) instead.
     */
    @Deprecated
    public void execute(RequestOtpRequest request) {
        if (!userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceNotFoundException("Email not registered");
        }

        String otp = otpRedisSchema.generateOtp(
                OtpRedisSchema.OtpPurpose.REGISTRATION, request.getEmail());
        notificationFacade.sendEmail(request.getEmail(), otp);

        log.info("OTP resent to: {}", request.getEmail());
    }
}
