package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.BadRequestException;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.cache.OtpRedisSchema;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.RequestOtpRequest;
import com.drc.aidbridge.modules.notification.NotificationFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestOtpUseCase {

    private final UserJpaRepository userRepository;
    private final OtpRedisSchema otpRedisSchema;
    private final NotificationFacade notificationFacade;

    public void execute(RequestOtpRequest request) {
        validateRequest(request);

        String identifier = getIdentifier(request);
        OtpRedisSchema.OtpPurpose purpose = mapOtpType(request.getOtpType());

        // Validate user exists for certain OTP types
        switch (purpose) {
            case PASSWORD_RESET -> {
                if (!userRepository.existsByEmail(identifier)) {
                    throw new ResourceNotFoundException("Email not registered");
                }
            }
            case REGISTRATION -> {

            }
            default -> {
                // Other types
            }
        }

        // Generate and send OTP
        String otp = otpRedisSchema.generateOtp(purpose, identifier);
        sendOtp(request, otp, purpose);

        log.info("OTP sent for {} to: {}", request.getOtpType(), identifier);
    }

    private void validateRequest(RequestOtpRequest request) {
        if (!StringUtils.hasText(request.getEmail()) && !StringUtils.hasText(request.getPhoneNumber())) {
            throw new BadRequestException("Either email or phone_number is required");
        }
    }

    private String getIdentifier(RequestOtpRequest request) {
        // Prefer email over phone
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

    private void sendOtp(RequestOtpRequest request, String otp, OtpRedisSchema.OtpPurpose purpose) {
        if (StringUtils.hasText(request.getEmail())) {
            switch (purpose) {
                case PASSWORD_RESET -> notificationFacade.sendPasswordResetEmail(request.getEmail(), otp);
                case REGISTRATION -> notificationFacade.sendEmail(request.getEmail(), otp);
                default -> notificationFacade.sendEmail(request.getEmail(), otp);
            }
        } else if (StringUtils.hasText(request.getPhoneNumber())) {
            // TODO: Implement SMS sending
            log.warn("SMS OTP not implemented yet, phone: {}", request.getPhoneNumber());
            throw new BadRequestException("SMS OTP not supported yet. Please use email.");
        }
    }
}
