package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.infrastructure.security.JwtService;
import com.drc.aidbridge.modules.user.internal.cache.SessionCacheRedisSchema;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.LogoutRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogoutUserUseCase {

    private final JwtService jwtService;
    private final UserJpaRepository userRepository;
    private final SessionCacheRedisSchema sessionCacheRedisSchema;

    public void execute(String authHeader, LogoutRequest request) {
        // Normalize and extract user information
        String refreshToken = request != null ? normalizeOptional(request.getRefreshToken()) : null;
        UUID userId = resolveUserId(authHeader, refreshToken);

        // OPTIMIZATION: Chỉ blacklist refresh token (TTL dài).
        // Access token có TTL ngắn (15 phút) nên để tự hết hạn, tránh ghi Redis không
        // cần thiết.

        if (refreshToken != null) {
            jwtService.revokeToken(refreshToken);
            log.info("Refresh token revoked during logout");
        }

        if (userId != null) {
            jwtService.revokeAllUserTokens(userId);
            clearUserSessionState(userId);
        }

        // Access token không cần blacklist - sẽ tự expire sau 15 phút
        log.info("User logged out (access token will expire naturally)");
    }

    private UUID resolveUserId(String authHeader, String refreshToken) {
        Optional<UUID> fromRefresh = extractUserIdFromRefreshToken(refreshToken);
        if (fromRefresh.isPresent()) {
            return fromRefresh.get();
        }
        return extractUserIdFromAccessHeader(authHeader).orElse(null);
    }

    private Optional<UUID> extractUserIdFromRefreshToken(String refreshToken) {
        if (refreshToken == null) {
            return Optional.empty();
        }
        try {
            Claims claims = jwtService.validateRefreshToken(refreshToken);
            return Optional.of(jwtService.extractUserId(claims));
        } catch (JwtException ignored) {
            return Optional.empty();
        }
    }

    private Optional<UUID> extractUserIdFromAccessHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        try {
            Claims claims = jwtService.validateAccessToken(authHeader.substring(7));
            return Optional.of(jwtService.extractUserId(claims));
        } catch (JwtException ignored) {
            return Optional.empty();
        }
    }

    private void clearUserSessionState(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setFcmToken(null);
            userRepository.save(user);

            // Session schema currently keys userId using least-significant UUID bits.
            sessionCacheRedisSchema.deleteSession(userId.getLeastSignificantBits());
        });
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
