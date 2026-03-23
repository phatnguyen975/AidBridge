package com.drc.aidbridge.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Redis schema for User Session Cache.
 *
 * Key pattern: session:{userId}
 * Value: JSON serialized UserSession object
 * TTL: 24 hours (sliding expiration)
 *
 * Use cases:
 * - Cache user profile data to reduce DB calls
 * - Track user online status
 * - Store user preferences and settings
 * - Track active devices/sessions
 */
@Slf4j
@Service
public class SessionCacheRedisSchema {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "session";
    private static final String ONLINE_KEY = "users:online";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final Duration ONLINE_TTL = Duration.ofMinutes(5);

    public SessionCacheRedisSchema(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSession implements Serializable {
        private Long userId;
        private String email;
        private String fullName;
        private String role;
        private String avatarUrl;
        private String deviceId;
        private String fcmToken;
        private Double lastLatitude;
        private Double lastLongitude;
        private Long lastActiveAt;
        private String preferences; // JSON string for flexible preferences
    }

    /**
     * Save or update user session.
     */
    public void saveSession(UserSession session) {
        try {
            String key = buildKey(session.getUserId());
            session.setLastActiveAt(Instant.now().toEpochMilli());
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key, json, DEFAULT_TTL);
            log.debug("Session saved for user {}", session.getUserId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize session for user {}", session.getUserId(), e);
        }
    }

    /**
     * Get user session by ID.
     */
    public Optional<UserSession> getSession(Long userId) {
        try {
            String key = buildKey(userId);
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                // Refresh TTL on access (sliding expiration)
                redisTemplate.expire(key, DEFAULT_TTL);
                return Optional.of(objectMapper.readValue(json, UserSession.class));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize session for user {}", userId, e);
        }
        return Optional.empty();
    }

    /**
     * Delete user session.
     */
    public void deleteSession(Long userId) {
        String key = buildKey(userId);
        redisTemplate.delete(key);
        setUserOffline(userId);
        log.debug("Session deleted for user {}", userId);
    }

    /**
     * Update user's last known location.
     */
    public void updateLocation(Long userId, Double latitude, Double longitude) {
        getSession(userId).ifPresent(session -> {
            session.setLastLatitude(latitude);
            session.setLastLongitude(longitude);
            session.setLastActiveAt(Instant.now().toEpochMilli());
            saveSession(session);
        });
    }

    /**
     * Update user's FCM token for push notifications.
     */
    public void updateFcmToken(Long userId, String fcmToken) {
        getSession(userId).ifPresent(session -> {
            session.setFcmToken(fcmToken);
            saveSession(session);
        });
    }

    /**
     * Mark user as online (heartbeat).
     */
    public void setUserOnline(Long userId) {
        redisTemplate.opsForZSet().add(
                ONLINE_KEY,
                String.valueOf(userId),
                Instant.now().toEpochMilli());
        // Clean up old entries
        long threshold = Instant.now().minusMillis(ONLINE_TTL.toMillis()).toEpochMilli();
        redisTemplate.opsForZSet().removeRangeByScore(ONLINE_KEY, 0, threshold);
    }

    /**
     * Mark user as offline.
     */
    public void setUserOffline(Long userId) {
        redisTemplate.opsForZSet().remove(ONLINE_KEY, String.valueOf(userId));
    }

    /**
     * Check if user is online.
     */
    public boolean isUserOnline(Long userId) {
        Double score = redisTemplate.opsForZSet().score(ONLINE_KEY, String.valueOf(userId));
        if (score == null)
            return false;
        long threshold = Instant.now().minusMillis(ONLINE_TTL.toMillis()).toEpochMilli();
        return score > threshold;
    }

    /**
     * Get all online user IDs.
     */
    public Set<String> getOnlineUsers() {
        long threshold = Instant.now().minusMillis(ONLINE_TTL.toMillis()).toEpochMilli();
        return redisTemplate.opsForZSet().rangeByScore(
                ONLINE_KEY,
                threshold,
                Double.MAX_VALUE);
    }

    /**
     * Get count of online users.
     */
    public long getOnlineUserCount() {
        long threshold = Instant.now().minusMillis(ONLINE_TTL.toMillis()).toEpochMilli();
        Long count = redisTemplate.opsForZSet().count(ONLINE_KEY, threshold, Double.MAX_VALUE);
        return count != null ? count : 0;
    }

    /**
     * Check if session exists.
     */
    public boolean hasSession(Long userId) {
        String key = buildKey(userId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private String buildKey(Long userId) {
        return String.format("%s:%d", KEY_PREFIX, userId);
    }
}
