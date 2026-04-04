package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.cache.OtpRedisSchema;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.ForgotPasswordRequest;
import com.drc.aidbridge.modules.notification.NotificationFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Deprecated
@Slf4j
@Component
@RequiredArgsConstructor
public class ForgotPasswordUseCase {

    private final UserJpaRepository userRepository;
    private final OtpRedisSchema otpRedisSchema;
    private final NotificationFacade notificationFacade;

    public void execute(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Email not registered"));

        String otp = otpRedisSchema.generateOtp(
                OtpRedisSchema.OtpPurpose.PASSWORD_RESET, request.getEmail());
        notificationFacade.sendPasswordResetEmail(request.getEmail(), otp);

        log.info("Password reset initiated for: {}", request.getEmail());
    }
}
