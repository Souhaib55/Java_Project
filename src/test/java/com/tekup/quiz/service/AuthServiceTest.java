package com.tekup.quiz.service;

import com.tekup.quiz.dao.UserDao;
import com.tekup.quiz.model.Role;
import com.tekup.quiz.model.User;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    @Test
    void registerAndLoginShouldSucceed() {
        UserDao userDao = new InMemoryUserDao();
        AuthService authService = new AuthService(userDao);

        User created = authService.register("asma", "secret1", Role.PLAYER);
        assertTrue(created.getId() > 0);
        assertNotNull(created.getPasswordHash());

        User logged = authService.login("asma", "secret1");
        assertEquals(created.getId(), logged.getId());
        assertEquals(Role.PLAYER, logged.getRole());
    }

    @Test
    void registerShouldRejectDuplicateUsername() {
        UserDao userDao = new InMemoryUserDao();
        AuthService authService = new AuthService(userDao);

        authService.register("asma", "secret1", Role.PLAYER);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register("asma", "secret2", Role.ADMIN)
        );
        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    void loginShouldFailWithInvalidPassword() {
        UserDao userDao = new InMemoryUserDao();
        AuthService authService = new AuthService(userDao);

        authService.register("asma", "secret1", Role.PLAYER);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login("asma", "wrongpw")
        );
        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void loginShouldTrimUsernameInput() {
        UserDao userDao = new InMemoryUserDao();
        AuthService authService = new AuthService(userDao);

        authService.register("asma", "secret1", Role.PLAYER);

        User logged = authService.login("  asma  ", "secret1");
        assertEquals("asma", logged.getUsername());
    }

    @Test
    void registerShouldTrimUsernameInput() {
        UserDao userDao = new InMemoryUserDao();
        AuthService authService = new AuthService(userDao);

        User created = authService.register("  asma  ", "secret1", Role.PLAYER);

        assertEquals("asma", created.getUsername());
    }

    private static class InMemoryUserDao implements UserDao {
        private final AtomicLong sequence = new AtomicLong(0);
        private final Map<Long, User> byId = new HashMap<>();
        private final Map<String, User> byUsername = new HashMap<>();

        @Override
        public Optional<User> findById(long id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.ofNullable(byUsername.get(username));
        }

        @Override
        public User save(User user) {
            if (user.getId() <= 0) {
                user.setId(sequence.incrementAndGet());
            }
            byId.put(user.getId(), user);
            byUsername.put(user.getUsername(), user);
            return user;
        }
    }
}
