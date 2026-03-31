package com.drc.aidbridge.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisConnection {

    private static RedisConnection instance;
    private final RedisConnectionFactory connectionFactory;
    private final StringRedisTemplate stringRedisTemplate;
    private boolean connected = false;

    @Autowired
    public RedisConnection(RedisConnectionFactory connectionFactory, StringRedisTemplate stringRedisTemplate) {
        this.connectionFactory = connectionFactory;
        this.stringRedisTemplate = stringRedisTemplate;
        instance = this;
    }

    public static RedisConnection getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RedisConnection has not been initialized yet");
        }
        return instance;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void testConnection() {
        try {
            String pong = stringRedisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();

            if ("PONG".equals(pong)) {
                connected = true;
                log.info("========================================");
                log.info("  REDIS CONNECTION SUCCESSFUL!");
                log.info("========================================");
                log.info("  Response: {}", pong);
                log.info("========================================");
            }
        } catch (Exception e) {
            connected = false;
            log.error("========================================");
            log.error("  REDIS CONNECTION FAILED!");
            log.error("========================================");
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

    public RedisConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }
}
