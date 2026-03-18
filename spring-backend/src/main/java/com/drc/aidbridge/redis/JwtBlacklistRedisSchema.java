package com.drc.aidbridge.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Redis schema for JWT Token Blacklist.
 *
 * Key pattern: jwt:blacklist:{jti}
 * Value: timestamp when blacklisted
 * TTL: remaining token expiration time
 *
 * Use cases:
 * - User logout (invalidate current token)
 * - Password change (invalidate all tokens)
 * - Account security (force re-login)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtBlacklistRedisSchema {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "jwt:blacklist";
    private static final String USER_TOKENS_PREFIX = "jwt:user";

    /**
     * Add a JWT token to the blacklist.
     * 
     * @param jti            JWT ID (unique token identifier)
     * @param expirationDate Token's original expiration date
     */
    public void blacklistToken(String jti, Date expirationDate) {
        String key = buildKey(jti);
        long ttlSeconds = calculateTtl(expirationDate);

        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(
                    key,
                    String.valueOf(Instant.now().toEpochMilli()),
                    Duration.ofSeconds(ttlSeconds));
            log.debug("Token {} blacklisted, TTL: {} seconds", jti, ttlSeconds);
        }
    }

    /**
     * Check if a token is blacklisted.
     * 
     * @param jti JWT ID
     * @return true if blacklisted, false otherwise
     */
    public boolean isBlacklisted(String jti) {
        String key = buildKey(jti);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Remove a token from blacklist (rarely needed).
     * 
     * @param jti JWT ID
     */
    public void removeFromBlacklist(String jti) {
        String key = buildKey(jti);
        redisTemplate.delete(key);
        log.debug("Token {} removed from blacklist", jti);
    }

    /**
     * Track user's active token (for invalidating all user tokens).
     * 
     * @param userId         User ID
     * @param jti            JWT ID
     * @param expirationDate Token expiration
     */
    public void trackUserToken(Long userId, String jti, Date expirationDate) {
        String key = buildUserKey(userId);
        long ttlSeconds = calculateTtl(expirationDate);

        if (ttlSeconds > 0) {
            redisTemplate.opsForSet().add(key, jti);
            redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
        }
    }

    /**
     * Blacklist all tokens for a user (e.g., password change, account compromise).
     * 
     * @param userId          User ID
     * @param tokenExpiration Default expiration for blacklist entries
     */
    public void blacklistAllUserTokens(Long userId, Duration tokenExpiration) {
        String userKey = buildUserKey(userId);
        var tokens = redisTemplate.opsForSet().members(userKey);

        if (tokens != null && !tokens.isEmpty()) {
            for (String jti : tokens) {
                String blacklistKey = buildKey(jti);
                redisTemplate.opsForValue().set(
                        blacklistKey,
                        String.valueOf(Instant.now().toEpochMilli()),
                        tokenExpiration);
            }
            redisTemplate.delete(userKey);
            log.info("All tokens blacklisted for user {}", userId);
        }
    }

    /**
     * Get count of blacklisted tokens (for monitoring).
     */
    public long getBlacklistCount() {
        var keys = redisTemplate.keys(KEY_PREFIX + ":*");
        return keys != null ? keys.size() : 0;
    }

    private String buildKey(String jti) {
        return String.format("%s:%s", KEY_PREFIX, jti);
    }

    private String buildUserKey(Long userId) {
        return String.format("%s:%d", USER_TOKENS_PREFIX, userId);
    }

    private long calculateTtl(Date expirationDate) {
        long now = Instant.now().toEpochMilli();
        long expiry = expirationDate.getTime();
        return Math.max(0, (expiry - now) / 1000);
    }
}
