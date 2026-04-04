package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.modules.shared.enums.UserRole;
import com.drc.aidbridge.modules.shared.exception.DuplicateResourceException;
import com.drc.aidbridge.modules.user.internal.cache.OtpRedisSchema;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.mapper.UserMapper;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.AuthResponse;
import com.drc.aidbridge.modules.user.internal.web.dto.RegisterRequest;
import com.drc.aidbridge.modules.notification.NotificationFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpRedisSchema otpRedisSchema;
    private final NotificationFacade notificationFacade;
    private final UserMapper userMapper;
  

    @Transactional
    public AuthResponse execute(RegisterRequest request) {
        // Validate email or phone required
        if (!StringUtils.hasText(request.getEmail()) && !StringUtils.hasText(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Either email or phone_number is required");
        }

        // Check duplicates
        if (StringUtils.hasText(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }
        if (StringUtils.hasText(request.getPhoneNumber()) &&
                userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateResourceException("Phone number already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.valueOf(request.getRole().toUpperCase()))
                .avatarUrl(request.getAvatarUrl())
                .isVerified(false)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {} with role {}", user.getEmail(), user.getRole());

        

        // Send verification OTP
        if (StringUtils.hasText(request.getEmail())) {
            String otp = otpRedisSchema.generateOtp(
                    OtpRedisSchema.OtpPurpose.REGISTRATION, request.getEmail());
            notificationFacade.sendEmail(request.getEmail(), otp);
        }

        return userMapper.buildAuthResponse(user, null, null);
    }
}
