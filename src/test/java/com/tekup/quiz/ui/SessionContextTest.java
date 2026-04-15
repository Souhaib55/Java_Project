package com.tekup.quiz.ui;

import com.tekup.quiz.model.Role;
import com.tekup.quiz.model.User;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionContextTest {

    @Test
    void sessionShouldExpireAfterConfiguredTimeout() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T10:00:00Z"));
        SessionContext sessionContext = new SessionContext(Duration.ofMinutes(15), now::get);

        sessionContext.setCurrentUser(new User("asma", "hash", Role.PLAYER));
        assertFalse(sessionContext.isSessionExpired());

        now.set(now.get().plus(Duration.ofMinutes(16)));

        assertTrue(sessionContext.isSessionExpired());
    }

    @Test
    void sessionActivityUpdateShouldRefreshExpiryWindow() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T10:00:00Z"));
        SessionContext sessionContext = new SessionContext(Duration.ofMinutes(15), now::get);

        sessionContext.setCurrentUser(new User("asma", "hash", Role.PLAYER));
        now.set(now.get().plus(Duration.ofMinutes(10)));
        sessionContext.setSelectedQuestionCount(8);

        now.set(Instant.parse("2026-01-01T10:24:00Z"));
        assertFalse(sessionContext.isSessionExpired());

        now.set(Instant.parse("2026-01-01T10:26:00Z"));
        assertTrue(sessionContext.isSessionExpired());
    }

    @Test
    void expireSessionShouldExposeOneTimeNoticeAndClearUser() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T10:00:00Z"));
        SessionContext sessionContext = new SessionContext(Duration.ofMinutes(15), now::get);

        sessionContext.setCurrentUser(new User("asma", "hash", Role.ADMIN));
        sessionContext.expireSession();

        assertNull(sessionContext.getCurrentUser());
        assertTrue(sessionContext.consumeSessionExpiredNotice());
        assertFalse(sessionContext.consumeSessionExpiredNotice());
    }
}
