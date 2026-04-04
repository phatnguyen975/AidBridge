package com.drc.aidbridge.modules.user.internal.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

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
    private static final String VERIFIED_SUFFIX = ":verified";

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

    public String generateOtp(OtpPurpose purpose, String identifier) {
        String otp = generateRandomOtp();
        String key = buildKey(purpose, identifier);

        redisTemplate.opsForValue().set(key, otp, DEFAULT_TTL);
        resetAttempts(purpose, identifier);

        log.debug("OTP generated for {}:{}", purpose.getValue(), identifier);
        return otp;
    }

    public boolean verifyOtp(OtpPurpose purpose, String identifier, String otp) {
        return verifyOtpInternal(purpose, identifier, otp, true);
    }

    /**
     * Verifies OTP without deleting it on success.
     * Useful for two-step flows where the same OTP is validated first,
     * then consumed by a later confirmation endpoint.
     */
    public boolean verifyOtpWithoutConsuming(OtpPurpose purpose, String identifier, String otp) {
        return verifyOtpInternal(purpose, identifier, otp, false);
    }

    public void markOtpVerified(OtpPurpose purpose, String identifier) {
        String verifiedKey = buildVerifiedKey(purpose, identifier);
        redisTemplate.opsForValue().set(verifiedKey, "true", DEFAULT_TTL);
    }

    public boolean isOtpVerified(OtpPurpose purpose, String identifier) {
        String verifiedKey = buildVerifiedKey(purpose, identifier);
        String value = redisTemplate.opsForValue().get(verifiedKey);
        return "true".equals(value);
    }

    public void clearOtpVerified(OtpPurpose purpose, String identifier) {
        String verifiedKey = buildVerifiedKey(purpose, identifier);
        redisTemplate.delete(verifiedKey);
    }

    private boolean verifyOtpInternal(OtpPurpose purpose,
            String identifier,
            String otp,
            boolean consumeOnSuccess) {
        if (isBlocked(purpose, identifier)) {
            log.warn("OTP verification blocked for {}:{} - too many attempts", purpose.getValue(), identifier);
            return false;
        }

        String key = buildKey(purpose, identifier);
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp != null && storedOtp.equals(otp)) {
            if (consumeOnSuccess) {
                redisTemplate.delete(key);
                deleteAttempts(purpose, identifier);
                clearOtpVerified(purpose, identifier);
            }
            log.debug("OTP verified successfully for {}:{}", purpose.getValue(), identifier);
            return true;
        }

        incrementAttempts(purpose, identifier);
        log.debug("Invalid OTP for {}:{}", purpose.getValue(), identifier);
        return false;
    }

    public boolean hasOtp(OtpPurpose purpose, String identifier) {
        String key = buildKey(purpose, identifier);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void deleteOtp(OtpPurpose purpose, String identifier) {
        String key = buildKey(purpose, identifier);
        redisTemplate.delete(key);
        deleteAttempts(purpose, identifier);
    }

    public Optional<Long> getRemainingTtl(OtpPurpose purpose, String identifier) {
        String key = buildKey(purpose, identifier);
        Long ttl = redisTemplate.getExpire(key);
        return ttl != null && ttl > 0 ? Optional.of(ttl) : Optional.empty();
    }

    public int getRemainingAttempts(OtpPurpose purpose, String identifier) {
        String key = buildAttemptKey(purpose, identifier);
        String attempts = redisTemplate.opsForValue().get(key);
        int used = attempts != null ? Integer.parseInt(attempts) : 0;
        return Math.max(0, MAX_ATTEMPTS - used);
    }

    private String buildKey(OtpPurpose purpose, String identifier) {
        return String.format("%s:%s:%s", KEY_PREFIX, purpose.getValue(), identifier);
    }

    private String buildAttemptKey(OtpPurpose purpose, String identifier) {
        return buildKey(purpose, identifier) + ATTEMPT_SUFFIX;
    }

    private String buildVerifiedKey(OtpPurpose purpose, String identifier) {
        return buildKey(purpose, identifier) + VERIFIED_SUFFIX;
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
}
