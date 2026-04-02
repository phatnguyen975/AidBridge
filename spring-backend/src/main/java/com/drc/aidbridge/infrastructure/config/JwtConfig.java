package com.drc.aidbridge.infrastructure.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT Configuration for token expiration settings.
 */
@Configuration
@Getter
public class JwtConfig {

    @Value("${jwt.access-token.expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token.expiration-ms:604800000}")
    private long refreshTokenExpirationMs;
}
