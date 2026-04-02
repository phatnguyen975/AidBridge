package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.infrastructure.security.JwtService;
import com.drc.aidbridge.modules.shared.exception.AuthenticationException;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.cache.SessionCacheRedisSchema;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.UpdateFcmTokenRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Updates the FCM token associated with the current authenticated user and device session.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateFcmTokenUseCase {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserJpaRepository userRepository;
    private final SessionCacheRedisSchema sessionCacheRedisSchema;

    /**
     * Validates the access token from Authorization header and updates both DB + Redis session token.
     */
    public void execute(String authHeader, UpdateFcmTokenRequest request) {
        String deviceId = normalizeOptional(request.getDeviceId());
        String fcmToken = normalizeOptional(request.getFcmToken());

        if (deviceId == null || fcmToken == null) {
            throw new AuthenticationException("Device ID and FCM token are required");
        }

        UUID userId = extractUserIdFromAccessHeader(authHeader);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFcmToken(fcmToken);
        userRepository.save(user);

        sessionCacheRedisSchema.updateFcmTokenByDeviceId(deviceId, fcmToken);

        log.debug("FCM token updated for user {} and device {}", userId, deviceId);
    }

    private UUID extractUserIdFromAccessHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new AuthenticationException("Unauthorized request");
        }

        try {
            Claims claims = jwtService.validateAccessToken(authHeader.substring(BEARER_PREFIX.length()));
            return jwtService.extractUserId(claims);
        } catch (JwtException e) {
            throw new AuthenticationException("Invalid or expired access token");
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
