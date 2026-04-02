package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.AuthenticationException;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.mapper.UserMapper;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.security.JwtService;
import com.drc.aidbridge.modules.user.internal.web.dto.AuthResponse;
import com.drc.aidbridge.modules.user.internal.web.dto.RefreshTokenRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final UserJpaRepository userRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthResponse execute(RefreshTokenRequest request) {
        Claims claims;
        try {
            claims = jwtService.validateRefreshToken(request.getRefreshToken());
        } catch (JwtException e) {
            throw new AuthenticationException("Invalid or expired refresh token");
        }

        UUID userId = jwtService.extractUserId(claims);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new AuthenticationException("Account is deactivated");
        }

        jwtService.revokeToken(request.getRefreshToken());

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());

        log.debug("Token refreshed for user: {}", user.getEmail());
        return userMapper.buildAuthResponse(user, newAccessToken, newRefreshToken);
    }
}
