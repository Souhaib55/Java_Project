package com.tekup.quiz.dao.jdbc;

import com.tekup.quiz.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

final class IntegrationTestDatabase {
    private IntegrationTestDatabase() {
    }

    static void resetSchema() {
        try (Connection connection = DatabaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS quiz_attempts");
            statement.execute("DROP TABLE IF EXISTS questions");
            statement.execute("DROP TABLE IF EXISTS categories");
            statement.execute("DROP TABLE IF EXISTS users");

            statement.execute("""
                    CREATE TABLE users (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(100) NOT NULL UNIQUE,
                        password_hash VARCHAR(255) NOT NULL,
                        role VARCHAR(20) NOT NULL DEFAULT 'PLAYER',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            statement.execute("""
                    CREATE TABLE categories (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL UNIQUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            statement.execute("""
                    CREATE TABLE questions (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        type VARCHAR(20) NOT NULL,
                        category_id BIGINT NOT NULL,
                        difficulty VARCHAR(20) NOT NULL,
                        prompt TEXT NOT NULL,
                        option_a VARCHAR(255),
                        option_b VARCHAR(255),
                        option_c VARCHAR(255),
                        option_d VARCHAR(255),
                        correct_answer VARCHAR(10) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_questions_category FOREIGN KEY (category_id) REFERENCES categories(id)
                    )
                    """);

            statement.execute("""
                    CREATE TABLE quiz_attempts (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        category_id BIGINT,
                        score INT NOT NULL,
                        total_questions INT NOT NULL,
                        attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_attempts_user FOREIGN KEY (user_id) REFERENCES users(id),
                        CONSTRAINT fk_attempts_category FOREIGN KEY (category_id) REFERENCES categories(id)
                    )
                    """);

            statement.execute("CREATE INDEX idx_questions_category_difficulty ON questions(category_id, difficulty)");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to reset integration test schema", exception);
        }
    }

    static long insertUser(String username, String role) {
        String sql = "INSERT INTO users(username, password_hash, role) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, "hash");
            statement.setString(3, role);
            statement.executeUpdate();
            return generatedId(statement);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to insert user seed", exception);
        }
    }

    static long insertCategory(String name) {
        String sql = "INSERT INTO categories(name) VALUES (?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.executeUpdate();
            return generatedId(statement);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to insert category seed", exception);
        }
    }

    static long insertQuestionMcq(long categoryId, String difficulty, String prompt, String correctAnswer) {
        String sql = """
                INSERT INTO questions(type, category_id, difficulty, prompt, option_a, option_b, option_c, option_d, correct_answer)
                VALUES ('MCQ', ?, ?, ?, 'A1', 'B1', 'C1', 'D1', ?)
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, categoryId);
            statement.setString(2, difficulty);
            statement.setString(3, prompt);
            statement.setString(4, correctAnswer);
            statement.executeUpdate();
            return generatedId(statement);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to insert MCQ seed", exception);
        }
    }

    static long insertQuestionTrueFalse(long categoryId, String difficulty, String prompt, String correctAnswer) {
        String sql = """
                INSERT INTO questions(type, category_id, difficulty, prompt, option_a, option_b, option_c, option_d, correct_answer)
                VALUES ('TRUE_FALSE', ?, ?, ?, 'TRUE', 'FALSE', NULL, NULL, ?)
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, categoryId);
            statement.setString(2, difficulty);
            statement.setString(3, prompt);
            statement.setString(4, correctAnswer);
            statement.executeUpdate();
            return generatedId(statement);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to insert TRUE_FALSE seed", exception);
        }
    }

    static long insertAttempt(long userId, Long categoryId, int score, int totalQuestions, LocalDateTime attemptedAt) {
        String sql = "INSERT INTO quiz_attempts(user_id, category_id, score, total_questions, attempted_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, userId);
            if (categoryId == null) {
                statement.setNull(2, java.sql.Types.BIGINT);
            } else {
                statement.setLong(2, categoryId);
            }
            statement.setInt(3, score);
            statement.setInt(4, totalQuestions);
            statement.setTimestamp(5, Timestamp.valueOf(attemptedAt));
            statement.executeUpdate();
            return generatedId(statement);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to insert attempt seed", exception);
        }
    }

    private static long generatedId(PreparedStatement statement) throws SQLException {
        try (var keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
            throw new IllegalStateException("No generated key returned");
        }
    }
}
