package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.infrastructure.security.JwtService;
import com.drc.aidbridge.modules.shared.enums.UserRole;
import com.drc.aidbridge.modules.shared.exception.AuthenticationException;
import com.drc.aidbridge.modules.shared.exception.BadRequestException;
import com.drc.aidbridge.modules.user.internal.cache.SessionCacheRedisSchema;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.mapper.UserMapper;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.AuthResponse;
import com.drc.aidbridge.modules.user.internal.web.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginUserUseCase {

    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionCacheRedisSchema sessionCacheRedisSchema;
    private final UserMapper userMapper;

    public AuthResponse execute(LoginRequest request) {
        // Validate: email OR phone required
        if (!StringUtils.hasText(request.getEmail()) && !StringUtils.hasText(request.getPhoneNumber())) {
            throw new BadRequestException("Either email or phone_number is required");
        }

        // Find user by email or phone
        User user = findUser(request);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("Account is deactivated");
        }

        if (!user.isVerified() && requiresVerificationBeforeLogin(user)) {
            throw new AuthenticationException("Account is not verified. Please verify OTP before login");
        }

        // Update FCM token if provided
        if (StringUtils.hasText(request.getFcmToken())) {
            user.setFcmToken(request.getFcmToken());
            userRepository.save(user);
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        userMapper.cacheUserSession(sessionCacheRedisSchema, user);

        log.info("User logged in: {}", user.getEmail() != null ? user.getEmail() : user.getPhoneNumber());
        return userMapper.buildAuthResponse(user, accessToken, refreshToken);
    }

    private User findUser(LoginRequest request) {
        if (StringUtils.hasText(request.getEmail())) {
            return userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new AuthenticationException("Invalid credentials"));
        }
        return userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));
    }

    private boolean requiresVerificationBeforeLogin(User user) {
        return user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.STAFF;
    }
}
