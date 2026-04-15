package com.tekup.quiz.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {
    private static final Properties PROPERTIES = loadProperties();

    private AppConfig() {
    }

    public static String get(String key) {
        String envKey = toEnvKey(key);
        String envValue = System.getenv(envKey);
        if (envValue != null) {
            return envValue.trim();
        }

        String propertyValue = PROPERTIES.getProperty(key);
        if (propertyValue == null) {
            return null;
        }
        return propertyValue.trim();
    }

    public static String require(String key) {
        String value = get(key);
        if (value == null) {
            throw new IllegalStateException(
                    "Missing required configuration: " + key + ". "
                            + "Configure it in application.properties or set environment variable "
                            + toEnvKey(key)
            );
        }
        return value;
    }

    private static String toEnvKey(String key) {
        return key.toUpperCase().replace('.', '_');
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (inputStream == null) {
                return properties;
            }
            properties.load(inputStream);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load application.properties", exception);
        }
    }
}
