package com.drc.aidbridge.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Registers a shared Jackson ObjectMapper for application services.
 */
@Configuration
public class JacksonConfig {

    /**
     * Exposes the primary ObjectMapper bean used by Spring-managed components.
     *
     * @return configured ObjectMapper instance
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}