package com.tekup.quiz.dao.jdbc;

import com.tekup.quiz.dao.UserDao;
import com.tekup.quiz.model.Role;
import com.tekup.quiz.model.User;
import com.tekup.quiz.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Optional;

public class JdbcUserDao implements UserDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcUserDao.class);

    @Override
    public Optional<User> findById(long id) {
        String sql = "SELECT id, username, password_hash, role FROM users WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            LOGGER.error("User query failed: operation=findById userId={}", id, exception);
            throw new IllegalStateException("Failed to find user by id", exception);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, role FROM users WHERE username = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            LOGGER.error("User query failed: operation=findByUsername username={}", username, exception);
            throw new IllegalStateException("Failed to find user by username", exception);
        }
    }

    @Override
    public User save(User user) {
        if (user.getId() > 0) {
            return update(user);
        }
        return insert(user);
    }

    private User insert(User user) {
        String sql = "INSERT INTO users(username, password_hash, role) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getRole().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
            return user;
        } catch (SQLException exception) {
            LOGGER.error("User insert failed: username={} role={}", user.getUsername(), user.getRole(), exception);
            throw mapInsertException(exception);
        }
    }

    private User update(User user) {
        String sql = "UPDATE users SET username = ?, password_hash = ?, role = ? WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getRole().name());
            statement.setLong(4, user.getId());
            statement.executeUpdate();
            return user;
        } catch (SQLException exception) {
            LOGGER.error("User update failed: userId={} username={} role={}", user.getId(), user.getUsername(), user.getRole(), exception);
            throw new IllegalStateException("Failed to update user", exception);
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        long id = resultSet.getLong("id");
        String username = resultSet.getString("username");
        String passwordHash = resultSet.getString("password_hash");
        Role role = Role.valueOf(resultSet.getString("role"));
        return new User(id, username, passwordHash, role);
    }

    private RuntimeException mapInsertException(SQLException exception) {
        if (isDuplicateKey(exception)) {
            return new IllegalArgumentException("Username already exists");
        }
        return new IllegalStateException("Failed to insert user", exception);
    }

    private boolean isDuplicateKey(SQLException exception) {
        String sqlState = exception.getSQLState();
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase(Locale.ROOT);
        return "23505".equals(sqlState)
                || exception.getErrorCode() == 1062
                || message.contains("duplicate")
                || message.contains("unique");
    }
}
