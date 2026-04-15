package com.tekup.quiz.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    void requireShouldReturnConfiguredValue() {
        String dbUrl = AppConfig.require("db.url");

        assertFalse(dbUrl.isBlank());
    }

    @Test
    void requireShouldAllowEmptyPasswordValue() {
        assertEquals("", AppConfig.require("db.password"));
    }

    @Test
    void requireShouldThrowForMissingKey() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> AppConfig.require("missing.key.for.tests")
        );

        assertTrue(exception.getMessage().contains("missing.key.for.tests"));
        assertTrue(exception.getMessage().contains("MISSING_KEY_FOR_TESTS"));
    }
}
