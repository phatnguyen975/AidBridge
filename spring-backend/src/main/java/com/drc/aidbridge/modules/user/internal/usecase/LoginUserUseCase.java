package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.exception.AuthenticationException;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.mapper.UserMapper;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.AuthResponse;
import com.drc.aidbridge.modules.user.internal.web.dto.LoginRequest;
import com.drc.aidbridge.redis.SessionCacheRedisSchema;
import com.drc.aidbridge.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("Account is deactivated");
        }

        if (request.getFcmToken() != null) {
            user.setFcmToken(request.getFcmToken());
            userRepository.save(user);
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        userMapper.cacheUserSession(sessionCacheRedisSchema, user);

        log.info("User logged in: {}", user.getEmail());
        return userMapper.buildAuthResponse(user, accessToken, refreshToken);
    }
}
