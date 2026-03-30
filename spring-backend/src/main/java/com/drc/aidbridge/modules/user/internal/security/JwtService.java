package com.drc.aidbridge.modules.user.internal.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String USER_TOKENS_PREFIX = "jwt:user_tokens:";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.jwt.secret:AidBridgeModularMonolithSecretKeyForJwtSigning1234567890}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    private SecretKey secretKey;

    public JwtService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    void init() {
        this.secretKey = Keys.hmacShaKeyFor(normalizeSecret(jwtSecret).getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String role) {
        return generateToken(userId, "access", role, accessTokenExpirationMs);
    }

    public String generateRefreshToken(UUID userId) {
        return generateToken(userId, "refresh", null, refreshTokenExpirationMs);
    }

    public Claims validateRefreshToken(String token) throws JwtException {
        Claims claims = parseToken(token);
        String jti = claims.getId();
        if (isBlacklisted(jti)) {
            throw new JwtException("Refresh token has been revoked");
        }

        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new JwtException("Invalid token type: expected refresh token");
        }

        return claims;
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public void revokeToken(String token) {
        try {
            Claims claims = parseToken(token);
            blacklistToken(claims.getId(), claims.getExpiration());
        } catch (JwtException e) {
            log.warn("Attempted to revoke invalid token: {}", e.getMessage());
        }
    }

    public void revokeAllUserTokens(UUID userId) {
        String key = userTokensKey(userId);
        Set<String> jtis = redisTemplate.opsForSet().members(key);
        if (jtis == null || jtis.isEmpty()) {
            return;
        }

        for (String jti : jtis) {
            redisTemplate.opsForValue().set(blacklistKey(jti), "1", Duration.ofMillis(refreshTokenExpirationMs));
        }
        redisTemplate.delete(key);
        log.info("All tokens revoked for user: {}", userId);
    }

    private String generateToken(UUID userId, String tokenType, String role, long expirationMs) {
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .subject(userId.toString())
                .claim("type", tokenType)
                .id(jti)
                .issuedAt(now)
            .expiration(expiration);

        if (role != null) {
            builder.claim("role", role);
        }

        String token = builder
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact();

        trackUserToken(userId, jti, expiration);
        return token;
    }

    private Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private void trackUserToken(UUID userId, String jti, Date expiration) {
        String key = userTokensKey(userId);
        redisTemplate.opsForSet().add(key, jti);

        long ttlMs = Math.max(1, expiration.getTime() - System.currentTimeMillis());
        redisTemplate.expire(key, Duration.ofMillis(ttlMs));
    }

    private void blacklistToken(String jti, Date expiration) {
        long ttlMs = Math.max(1, expiration.getTime() - System.currentTimeMillis());
        redisTemplate.opsForValue().set(blacklistKey(jti), "1", Duration.ofMillis(ttlMs));
    }

    private boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey(jti)));
    }

    private String userTokensKey(UUID userId) {
        return USER_TOKENS_PREFIX + userId;
    }

    private String blacklistKey(String jti) {
        return BLACKLIST_PREFIX + jti;
    }

    private String normalizeSecret(String secret) {
        String value = secret == null ? "" : secret;
        if (value.length() >= 32) {
            return value;
        }
        StringBuilder builder = new StringBuilder(value);
        while (builder.length() < 32) {
            builder.append('0');
        }
        return builder.toString();
    }
}
