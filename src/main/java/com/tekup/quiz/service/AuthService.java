package com.tekup.quiz.service;

import com.tekup.quiz.dao.UserDao;
import com.tekup.quiz.model.Role;
import com.tekup.quiz.model.User;
import com.tekup.quiz.util.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Objects;

public class AuthService {
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = Objects.requireNonNull(userDao, "userDao must not be null");
    }

    public User register(String username, String plainPassword, Role role) {
        String normalizedUsername = normalizeUsername(username);
        Role targetRole = Objects.requireNonNull(role, "role must not be null");
        validatePassword(plainPassword);

        LOGGER.info("Registration requested: username={} role={}", normalizedUsername, targetRole);

        if (userDao.findByUsername(normalizedUsername).isPresent()) {
            LOGGER.warn("Registration rejected: duplicate username={}", normalizedUsername);
            throw new IllegalArgumentException("Username already exists");
        }

        String hash = PasswordHasher.hash(plainPassword);
        User user = new User(normalizedUsername, hash, targetRole);
        User saved = userDao.save(user);
        LOGGER.info("Registration successful: userId={} username={} role={}", saved.getId(), saved.getUsername(), saved.getRole());
        return saved;
    }

    public User login(String username, String plainPassword) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(plainPassword);

        LOGGER.info("Login requested: username={}", normalizedUsername);

        Optional<User> userOptional = userDao.findByUsername(normalizedUsername);
        if (userOptional.isEmpty()) {
            LOGGER.warn("Login rejected: unknown username={}", normalizedUsername);
            throw new IllegalArgumentException("Invalid username or password");
        }

        User user = userOptional.get();

        if (!PasswordHasher.matches(plainPassword, user.getPasswordHash())) {
            LOGGER.warn("Login rejected: invalid password for username={}", normalizedUsername);
            throw new IllegalArgumentException("Invalid username or password");
        }

        LOGGER.info("Login successful: userId={} username={} role={}", user.getId(), user.getUsername(), user.getRole());
        return user;
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        return username.trim();
    }

    private void validatePassword(String plainPassword) {
        if (plainPassword == null || plainPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must contain at least 6 characters");
        }
    }
}
