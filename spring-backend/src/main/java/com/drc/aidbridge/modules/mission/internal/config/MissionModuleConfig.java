package com.drc.aidbridge.modules.mission.internal.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ComponentScan(basePackages = "com.drc.aidbridge.modules.mission")
public class MissionModuleConfig {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initializeSchema() {
        log.info("Ensuring mission schema supports dispatch failure status...");
        jdbcTemplate.execute("ALTER TYPE mission_status ADD VALUE IF NOT EXISTS 'DISPATCH_FAILED'");
        log.info("Mission schema validation completed.");
    }
}
