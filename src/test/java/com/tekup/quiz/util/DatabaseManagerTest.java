package com.tekup.quiz.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DatabaseManagerTest {

    @Test
    void verifyConnectionShouldSucceedWithTestConfiguration() {
        assertDoesNotThrow(DatabaseManager::verifyConnection);
    }
}
