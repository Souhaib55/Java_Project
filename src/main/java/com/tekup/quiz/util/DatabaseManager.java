package com.tekup.quiz.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);
    private static volatile HikariDataSource dataSource;

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static void verifyConnection() {
        try (Connection ignored = getConnection()) {
            // Fail fast at startup with a clear message when DB connectivity is broken.
            LOGGER.info("Database connection verification successful");
        } catch (SQLException exception) {
            LOGGER.error("Database connection verification failed", exception);
            throw new IllegalStateException(
                    "Failed to connect to database. Ensure the DB server is reachable and credentials are valid.",
                    exception
            );
        }
    }

    public static void shutdown() {
        synchronized (DatabaseManager.class) {
            if (dataSource != null) {
                LOGGER.info("Shutting down database pool");
                dataSource.close();
                dataSource = null;
            }
        }
    }

    private static HikariDataSource getDataSource() {
        HikariDataSource local = dataSource;
        if (local != null) {
            return local;
        }

        synchronized (DatabaseManager.class) {
            if (dataSource == null) {
                dataSource = buildDataSource();
            }
            return dataSource;
        }
    }

    private static HikariDataSource buildDataSource() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("quiz-db-pool");
        String jdbcUrl = AppConfig.require("db.url");
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(AppConfig.require("db.username"));
        config.setPassword(AppConfig.require("db.password"));
        int maxSize = readInt("db.pool.maxSize", 10, 1);
        int minIdle = readInt("db.pool.minIdle", 2, 0);
        long connectionTimeout = readLong("db.pool.connectionTimeoutMs", 10000L, 1000L);
        long validationTimeout = readLong("db.pool.validationTimeoutMs", 5000L, 1000L);
        long idleTimeout = readLong("db.pool.idleTimeoutMs", 600000L, 10000L);
        long maxLifetime = readLong("db.pool.maxLifetimeMs", 1800000L, 30000L);

        config.setMaximumPoolSize(maxSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setValidationTimeout(validationTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);

        LOGGER.info(
                "Initializing database pool: url={} maxSize={} minIdle={} connectionTimeoutMs={} validationTimeoutMs={}",
                maskJdbcUrl(jdbcUrl),
                maxSize,
                minIdle,
                connectionTimeout,
                validationTimeout
        );
        return new HikariDataSource(config);
    }

    private static String maskJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return "unknown";
        }
        return jdbcUrl
                .replaceAll("(?i)(password=)[^&;]+", "$1***")
                .replaceAll("(?i)(passwd=)[^&;]+", "$1***");
    }

    private static int readInt(String key, int defaultValue, int minValue) {
        String configured = AppConfig.get(key);
        if (configured == null) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(configured);
            return Math.max(parsed, minValue);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static long readLong(String key, long defaultValue, long minValue) {
        String configured = AppConfig.get(key);
        if (configured == null) {
            return defaultValue;
        }
        try {
            long parsed = Long.parseLong(configured);
            return Math.max(parsed, minValue);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
