package com.drc.aidbridge.modules.user.internal.mapper;

import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.internal.cache.SessionCacheRedisSchema;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.web.dto.AuthResponse;
import com.drc.aidbridge.modules.user.internal.web.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    // Entity → Public module DTO (for cross-module communication)
    public UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId().toString())
                .name(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .isVerified(user.isVerified())
                .build();
    }

    // Entity → API response DTO (for REST endpoints)
    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId().toString())
                .name(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .isVerified(user.isVerified())
                .build();
    }

    // Build full auth response with tokens and user info
    public AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(toResponse(user))
                .build();
    }

    // Cache user session to Redis for fast lookups
    public void cacheUserSession(SessionCacheRedisSchema sessionCacheRedisSchema, User user) {
        cacheUserSession(sessionCacheRedisSchema, user, null, user.getFcmToken());
    }

    // Cache user session with explicit device + FCM context from login payload.
    public void cacheUserSession(SessionCacheRedisSchema sessionCacheRedisSchema,
                                 User user,
                                 String deviceId,
                                 String fcmToken) {
        sessionCacheRedisSchema.saveSession(
                SessionCacheRedisSchema.UserSession.builder()
                        .userId(user.getId().getLeastSignificantBits())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole().name())
                        .avatarUrl(user.getAvatarUrl())
                        .deviceId(deviceId)
                        .fcmToken(fcmToken)
                        .build());
    }
}
