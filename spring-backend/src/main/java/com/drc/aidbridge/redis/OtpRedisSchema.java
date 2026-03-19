package com.drc.aidbridge.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

/**
 * Redis schema for OTP (One-Time Password) storage.
 *
 * Key pattern: otp:{purpose}:{identifier}
 * Value: 6-digit OTP code
 * TTL: 5 minutes (configurable)
 *
 * Purposes:
 * - registration: Email verification during sign up
 * - password_reset: Password reset verification
 * - login: 2FA login verification
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpRedisSchema {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "otp";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);
    private static final int OTP_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 5;
    private static final String ATTEMPT_SUFFIX = ":attempts";

    public enum OtpPurpose {
        REGISTRATION("registration"),
        PASSWORD_RESET("password_reset"),
        LOGIN("login");

        private final String value;

        OtpPurpose(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Generate and store a new OTP.
     * 
     * @param purpose    The purpose of OTP (registration, password_reset, login)
     * @param identifier User identifier (email or phone)
     * @return The generated OTP code
     */
    public String generateOtp(OtpPurpose purpose, String identifier) {
        String otp = generateRandomOtp();
        String key = buildKey(purpose, identifier);

        redisTemplate.opsForValue().set(key, otp, DEFAULT_TTL);
        resetAttempts(purpose, identifier);

        log.debug("OTP generated for {}:{}", purpose.getValue(), identifier);
        return otp;
    }

    /**
     * Verify an OTP code.
     * 
     * @param purpose    The purpose of OTP
     * @param identifier User identifier
     * @param otp        The OTP code to verify
     * @return true if valid, false otherwise
     */
    public boolean verifyOtp(OtpPurpose purpose, String identifier, String otp) {
        if (isBlocked(purpose, identifier)) {
            log.warn("OTP verification blocked for {}:{} - too many attempts", purpose.getValue(), identifier);
            return false;
        }

        String key = buildKey(purpose, identifier);
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp != null && storedOtp.equals(otp)) {
            // OTP is valid, delete it
            redisTemplate.delete(key);
            deleteAttempts(purpose, identifier);
            log.debug("OTP verified successfully for {}:{}", purpose.getValue(), identifier);
            return true;
        }

        // Invalid OTP, increment attempts
        incrementAttempts(purpose, identifier);
        log.debug("Invalid OTP for {}:{}", purpose.getValue(), identifier);
        return false;
    }

    /**
     * Check if OTP exists for given identifier.
     */
    public boolean hasOtp(OtpPurpose purpose, String identifier) {
        String key = buildKey(purpose, identifier);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Delete OTP for given identifier.
     */
    public void deleteOtp(OtpPurpose purpose, String identifier) {
        String key = buildKey(purpose, identifier);
        redisTemplate.delete(key);
        deleteAttempts(purpose, identifier);
    }

    /**
     * Get remaining TTL for OTP in seconds.
     */
    public Optional<Long> getRemainingTtl(OtpPurpose purpose, String identifier) {
        String key = buildKey(purpose, identifier);
        Long ttl = redisTemplate.getExpire(key);
        return ttl != null && ttl > 0 ? Optional.of(ttl) : Optional.empty();
    }

    private String buildKey(OtpPurpose purpose, String identifier) {
        return String.format("%s:%s:%s", KEY_PREFIX, purpose.getValue(), identifier);
    }

    private String buildAttemptKey(OtpPurpose purpose, String identifier) {
        return buildKey(purpose, identifier) + ATTEMPT_SUFFIX;
    }

    private String generateRandomOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    private void incrementAttempts(OtpPurpose purpose, String identifier) {
        String key = buildAttemptKey(purpose, identifier);
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofMinutes(30));
    }

    private void resetAttempts(OtpPurpose purpose, String identifier) {
        String key = buildAttemptKey(purpose, identifier);
        redisTemplate.delete(key);
    }

    private void deleteAttempts(OtpPurpose purpose, String identifier) {
        String key = buildAttemptKey(purpose, identifier);
        redisTemplate.delete(key);
    }

    private boolean isBlocked(OtpPurpose purpose, String identifier) {
        String key = buildAttemptKey(purpose, identifier);
        String attempts = redisTemplate.opsForValue().get(key);
        return attempts != null && Integer.parseInt(attempts) >= MAX_ATTEMPTS;
    }

    public int getRemainingAttempts(OtpPurpose purpose, String identifier) {
        String key = buildAttemptKey(purpose, identifier);
        String attempts = redisTemplate.opsForValue().get(key);
        int used = attempts != null ? Integer.parseInt(attempts) : 0;
        return Math.max(0, MAX_ATTEMPTS - used);
    }
}
