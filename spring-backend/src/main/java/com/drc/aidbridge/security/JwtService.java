package com.drc.aidbridge.security;

import com.drc.aidbridge.config.JwtConfig;
import com.drc.aidbridge.redis.JwtBlacklistRedisSchema;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

/**
 * JWT Service for RSA-signed token operations.
 *
 * - Access tokens: RS256 signed, 15-minute TTL
 * - Refresh tokens: RS512 signed, 7-day TTL
 *
 * Integrates with Redis for token blacklisting and user token tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final RsaKeyProperties rsaKeyProperties;
    private final JwtConfig jwtConfig;
    private final JwtBlacklistRedisSchema jwtBlacklistRedisSchema;

    // ==================== TOKEN GENERATION ====================

    /**
     * Generate access token with RS256 signature.
     * Claims: sub (userId), role, jti, type, iat, exp
     *
     * @param userId User's UUID
     * @param role   User's role (VICTIM, VOLUNTEER, SPONSOR, STAFF, ADMIN)
     * @return Signed JWT access token
     */
    public String generateAccessToken(UUID userId, String role) {
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getAccessTokenExpirationMs());

        String token = Jwts.builder()
                .header().add("typ", "JWT").and()
                .subject(userId.toString())
                .claim("role", role)
                .claim("type", "access")
                .id(jti)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(rsaKeyProperties.privateKey(), Jwts.SIG.RS256)
                .compact();

        // Track token for mass revocation capability
        jwtBlacklistRedisSchema.trackUserToken(
                userId.getLeastSignificantBits(), jti, expiration);

        log.debug("Generated access token for user {} with jti {}", userId, jti);
        return token;
    }

    /**
     * Generate refresh token with RS512 signature.
     * RS512 provides stronger security for long-lived tokens.
     *
     * @param userId User's UUID
     * @return Signed JWT refresh token
     */
    public String generateRefreshToken(UUID userId) {
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getRefreshTokenExpirationMs());

        String token = Jwts.builder()
                .header().add("typ", "JWT").and()
                .subject(userId.toString())
                .claim("type", "refresh")
                .id(jti)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(rsaKeyProperties.privateKey(), Jwts.SIG.RS512)
                .compact();

        // Track refresh token for user
        jwtBlacklistRedisSchema.trackUserToken(
                userId.getLeastSignificantBits(), jti, expiration);

        log.debug("Generated refresh token for user {} with jti {}", userId, jti);
        return token;
    }

    // ==================== TOKEN VALIDATION ====================

    /**
     * Validate access token and return claims if valid.
     * Checks: RSA signature, expiration, blacklist status, token type
     *
     * @param token JWT access token
     * @return Parsed claims
     * @throws JwtException if validation fails
     */
    public Claims validateAccessToken(String token) throws JwtException {
        Claims claims = parseToken(token);

        // Check if token is blacklisted
        String jti = claims.getId();
        if (jwtBlacklistRedisSchema.isBlacklisted(jti)) {
            throw new JwtException("Token has been revoked");
        }

        // Verify token type
        String type = claims.get("type", String.class);
        if (!"access".equals(type)) {
            throw new JwtException("Invalid token type: expected access token");
        }

        return claims;
    }

    /**
     * Validate refresh token for token renewal.
     *
     * @param token JWT refresh token
     * @return Parsed claims
     * @throws JwtException if validation fails
     */
    public Claims validateRefreshToken(String token) throws JwtException {
        Claims claims = parseToken(token);

        String jti = claims.getId();
        if (jwtBlacklistRedisSchema.isBlacklisted(jti)) {
            throw new JwtException("Refresh token has been revoked");
        }

        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new JwtException("Invalid token type: expected refresh token");
        }

        return claims;
    }

    /**
     * Parse and verify JWT token signature using RSA public key.
     */
    private Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(rsaKeyProperties.publicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ==================== TOKEN EXTRACTION ====================

    /**
     * Extract user ID from claims.
     */
    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extract user role from claims.
     */
    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }

    /**
     * Extract JWT ID from claims.
     */
    public String extractJti(Claims claims) {
        return claims.getId();
    }

    /**
     * Extract expiration date from claims.
     */
    public Date extractExpiration(Claims claims) {
        return claims.getExpiration();
    }

    // ==================== TOKEN REVOCATION ====================

    /**
     * Blacklist a specific token (logout single session).
     *
     * @param token JWT token to revoke
     */
    public void revokeToken(String token) {
        try {
            Claims claims = parseToken(token);
            jwtBlacklistRedisSchema.blacklistToken(
                    claims.getId(), claims.getExpiration());
            log.info("Token revoked: jti={}", claims.getId());
        } catch (JwtException e) {
            log.warn("Attempted to revoke invalid token: {}", e.getMessage());
        }
    }

    /**
     * Blacklist all tokens for a user.
     * Use case: password change, account compromise, force logout all devices.
     *
     * @param userId User's UUID
     */
    public void revokeAllUserTokens(UUID userId) {
        jwtBlacklistRedisSchema.blacklistAllUserTokens(
                userId.getLeastSignificantBits(),
                java.time.Duration.ofMillis(jwtConfig.getRefreshTokenExpirationMs()));
        log.info("All tokens revoked for user: {}", userId);
    }
}
