package com.drc.aidbridge.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

@Slf4j
@Component
public class SupabaseConnection {

    private static SupabaseConnection instance;
    private final DataSource dataSource;
    private boolean connected = false;

    @Autowired
    public SupabaseConnection(DataSource dataSource) {
        this.dataSource = dataSource;
        instance = this;
    }

    public static SupabaseConnection getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SupabaseConnection has not been initialized yet");
        }
        return instance;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void testConnection() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                DatabaseMetaData metaData = connection.getMetaData();
                connected = true;
                log.info("  SUPABASE CONNECTION SUCCESSFUL!");
            }
        } catch (SQLException e) {
            connected = false;
            log.error("  SUPABASE CONNECTION FAILED!");
            log.error("  Error: {}", e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
