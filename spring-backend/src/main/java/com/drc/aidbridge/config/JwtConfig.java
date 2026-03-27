package com.drc.aidbridge.config;

import com.drc.aidbridge.security.RsaKeyProperties;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT Configuration for token expiration settings.
 */
@Configuration
@EnableConfigurationProperties(RsaKeyProperties.class)
@Getter
public class JwtConfig {

    @Value("${jwt.access-token.expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token.expiration-ms:604800000}")
    private long refreshTokenExpirationMs;
}
