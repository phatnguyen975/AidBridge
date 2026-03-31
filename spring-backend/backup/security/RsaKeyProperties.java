package com.drc.aidbridge.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * RSA Key pair configuration properties.
 * Loads RSA public/private keys from PEM files for JWT signing.
 */
@ConfigurationProperties(prefix = "jwt.rsa")
public record RsaKeyProperties(
                RSAPublicKey publicKey,
                RSAPrivateKey privateKey) {
}
