package com.drc.aidbridge.modules.attachment.internal.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloudinaryConnection {

    private final Cloudinary cloudinary;
    private boolean connected = false;

    @EventListener(ApplicationReadyEvent.class)
    public void testConnection() {
        try {
            ApiResponse pingResponse = cloudinary.api().ping(Collections.emptyMap());

            connected = true;
            log.info("========================================");
            log.info("  CLOUDINARY CONNECTION SUCCESSFUL!");
            log.info("========================================");
            log.info("  Cloud name: {}", safeValue(cloudinary.config.cloudName));
            log.info("  API key: {}", maskValue(cloudinary.config.apiKey));
            log.info("  Ping response: {}", pingResponse);
            log.info("========================================");
        } catch (Exception e) {
            connected = false;
            log.error("========================================");
            log.error("  CLOUDINARY CONNECTION FAILED!");
            log.error("========================================");
            log.error("  Cloud name: {}", safeValue(cloudinary.config.cloudName));
            log.error("  API key: {}", maskValue(cloudinary.config.apiKey));
            log.error("  Error: {}", e.getMessage());
            log.error("  Exception Type: {}", e.getClass().getSimpleName());
            if (e.getCause() != null) {
                log.error("  Root Cause: {}", e.getCause().getMessage());
            }
            log.error("========================================");
        }
    }

    public boolean isConnected() {
        return connected;
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
