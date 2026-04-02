package com.drc.aidbridge.modules.attachment.internal.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConfigurationProperties(prefix = "cloudinary")
@Getter
@Setter
@Slf4j
public class CloudinaryConfig {

    private String cloudName;
    private String apiKey;
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));

        log.info(
                "Initialized Cloudinary client: cloudName={}, apiKey={}, apiSecretPresent={}, secure={}",
                safeValue(cloudName),
                maskValue(apiKey),
                StringUtils.hasText(apiSecret),
                true);

        return cloudinary;
    }

    private String safeValue(String value) {
        return StringUtils.hasText(value) ? value : "<empty>";
    }

    private String maskValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "<empty>";
        }
        if (value.length() <= 4) {
            return "*".repeat(value.length());
        }

        return value.substring(0, 2)
                + "*".repeat(value.length() - 4)
                + value.substring(value.length() - 2);
    }
}
