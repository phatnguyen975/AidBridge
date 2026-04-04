package com.drc.aidbridge.modules.user.internal.cache;

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
        private String preferences;
    }

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

    public Optional<UserSession> getSession(Long userId) {
        try {
            String key = buildKey(userId);
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                redisTemplate.expire(key, DEFAULT_TTL);
                return Optional.of(objectMapper.readValue(json, UserSession.class));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize session for user {}", userId, e);
        }
        return Optional.empty();
    }

    public void deleteSession(Long userId) {
        String key = buildKey(userId);
        redisTemplate.delete(key);
        setUserOffline(userId);
        log.debug("Session deleted for user {}", userId);
    }

    public void updateLocation(Long userId, Double latitude, Double longitude) {
        getSession(userId).ifPresent(session -> {
            session.setLastLatitude(latitude);
            session.setLastLongitude(longitude);
            session.setLastActiveAt(Instant.now().toEpochMilli());
            saveSession(session);
        });
    }

    public void updateFcmToken(Long userId, String fcmToken) {
        getSession(userId).ifPresent(session -> {
            session.setFcmToken(fcmToken);
            saveSession(session);
        });
    }

    public void updateFcmTokenByDeviceId(String deviceId, String fcmToken) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }

        Set<String> keys = redisTemplate.keys(KEY_PREFIX + ":*");
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                continue;
            }

            try {
                UserSession session = objectMapper.readValue(json, UserSession.class);
                if (deviceId.equals(session.getDeviceId())) {
                    session.setFcmToken(fcmToken);
                    saveSession(session);
                    return;
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse session key {} while updating FCM token", key, e);
            }
        }
    }

    public void setUserOnline(Long userId) {
        redisTemplate.opsForZSet().add(
                ONLINE_KEY,
                String.valueOf(userId),
                Instant.now().toEpochMilli());
        long threshold = Instant.now().minusMillis(ONLINE_TTL.toMillis()).toEpochMilli();
        redisTemplate.opsForZSet().removeRangeByScore(ONLINE_KEY, 0, threshold);
    }

    public void setUserOffline(Long userId) {
        redisTemplate.opsForZSet().remove(ONLINE_KEY, String.valueOf(userId));
    }

    public boolean isUserOnline(Long userId) {
        Double score = redisTemplate.opsForZSet().score(ONLINE_KEY, String.valueOf(userId));
        if (score == null)
            return false;
        long threshold = Instant.now().minusMillis(ONLINE_TTL.toMillis()).toEpochMilli();
        return score > threshold;
    }

    public Set<String> getOnlineUsers() {
        long threshold = Instant.now().minusMillis(ONLINE_TTL.toMillis()).toEpochMilli();
        return redisTemplate.opsForZSet().rangeByScore(
                ONLINE_KEY,
                threshold,
                Double.MAX_VALUE);
    }

    public long getOnlineUserCount() {
        long threshold = Instant.now().minusMillis(ONLINE_TTL.toMillis()).toEpochMilli();
        Long count = redisTemplate.opsForZSet().count(ONLINE_KEY, threshold, Double.MAX_VALUE);
        return count != null ? count : 0;
    }

    public boolean hasSession(Long userId) {
        String key = buildKey(userId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private String buildKey(Long userId) {
        return String.format("%s:%d", KEY_PREFIX, userId);
    }
}
