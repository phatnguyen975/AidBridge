package com.drc.aidbridge.modules.volunteer.internal.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ComponentScan(basePackages = "com.drc.aidbridge.modules.volunteer")
public class VolunteerModuleConfig {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initializeSchema() {
        try {
            log.info("Ensuring volunteer_profiles schema supports H3 Spatial Indexing...");
            jdbcTemplate.execute("ALTER TABLE volunteer_profiles ADD COLUMN IF NOT EXISTS h3_index VARCHAR(15)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_volunteer_h3_index ON volunteer_profiles(h3_index)");
            log.info("Schema validation completed for H3 Spatial Indexing.");
        } catch (Exception e) {
            log.warn("Failed to run dynamic H3 schema updates (it may already be configured): {}", e.getMessage());
        }
    }
}
