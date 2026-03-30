package com.drc.aidbridge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI aidBridgeOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("AidBridge API")
                .version("v1")
                .description("Disaster Relief Coordinator API")
                .contact(new Contact().name("AidBridge Team")));
    }
}
