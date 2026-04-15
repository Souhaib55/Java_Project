package com.tekup.quiz.dao.jdbc;

import com.tekup.quiz.dao.QuizAttemptDao;
import com.tekup.quiz.model.AttemptView;
import com.tekup.quiz.model.QuizAttempt;
import com.tekup.quiz.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcQuizAttemptDao implements QuizAttemptDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcQuizAttemptDao.class);

    @Override
    public QuizAttempt save(QuizAttempt attempt) {
        String sql = "INSERT INTO quiz_attempts(user_id, category_id, score, total_questions, attempted_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, attempt.getUserId());
            if (attempt.getCategoryId() == null) {
                statement.setNull(2, java.sql.Types.BIGINT);
            } else {
                statement.setLong(2, attempt.getCategoryId());
            }
            statement.setInt(3, attempt.getScore());
            statement.setInt(4, attempt.getTotalQuestions());
            statement.setTimestamp(5, Timestamp.valueOf(attempt.getAttemptedAt()));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    attempt.setId(keys.getLong(1));
                }
            }

            return attempt;
        } catch (SQLException exception) {
            LOGGER.error(
                    "Quiz attempt save failed: userId={} categoryId={} score={} totalQuestions={}",
                    attempt.getUserId(),
                    attempt.getCategoryId(),
                    attempt.getScore(),
                    attempt.getTotalQuestions(),
                    exception
            );
            throw new IllegalStateException("Failed to save quiz attempt", exception);
        }
    }

    @Override
    public List<QuizAttempt> findByUser(long userId) {
        String sql = "SELECT * FROM quiz_attempts WHERE user_id = ? ORDER BY attempted_at DESC";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAttempts(resultSet);
            }
        } catch (SQLException exception) {
            LOGGER.error("Quiz attempt query failed: operation=findByUser userId={}", userId, exception);
            throw new IllegalStateException("Failed to load user attempts", exception);
        }
    }

    @Override
    public List<QuizAttempt> findTopGlobal(int limit) {
        String sql = "SELECT * FROM quiz_attempts ORDER BY score DESC, attempted_at DESC LIMIT ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAttempts(resultSet);
            }
        } catch (SQLException exception) {
            LOGGER.error("Quiz attempt query failed: operation=findTopGlobal limit={}", limit, exception);
            throw new IllegalStateException("Failed to load global leaderboard", exception);
        }
    }

    @Override
    public List<QuizAttempt> findTopByCategory(long categoryId, int limit) {
        String sql = "SELECT * FROM quiz_attempts WHERE category_id = ? ORDER BY score DESC, attempted_at DESC LIMIT ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, categoryId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAttempts(resultSet);
            }
        } catch (SQLException exception) {
            LOGGER.error(
                    "Quiz attempt query failed: operation=findTopByCategory categoryId={} limit={}",
                    categoryId,
                    limit,
                    exception
            );
            throw new IllegalStateException("Failed to load category leaderboard", exception);
        }
    }

    @Override
    public List<AttemptView> findTopGlobalView(int limit) {
        String sql = """
                SELECT qa.id, qa.user_id, u.username, qa.category_id, c.name AS category_name,
                       qa.score, qa.total_questions, qa.attempted_at
                FROM quiz_attempts qa
                JOIN users u ON u.id = qa.user_id
                LEFT JOIN categories c ON c.id = qa.category_id
                ORDER BY qa.score DESC, qa.attempted_at DESC
                LIMIT ?
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAttemptViews(resultSet);
            }
        } catch (SQLException exception) {
            LOGGER.error("Quiz attempt view query failed: operation=findTopGlobalView limit={}", limit, exception);
            throw new IllegalStateException("Failed to load global leaderboard rows", exception);
        }
    }

    @Override
    public List<AttemptView> findTopByCategoryView(long categoryId, int limit) {
        String sql = """
                SELECT qa.id, qa.user_id, u.username, qa.category_id, c.name AS category_name,
                       qa.score, qa.total_questions, qa.attempted_at
                FROM quiz_attempts qa
                JOIN users u ON u.id = qa.user_id
                LEFT JOIN categories c ON c.id = qa.category_id
                WHERE qa.category_id = ?
                ORDER BY qa.score DESC, qa.attempted_at DESC
                LIMIT ?
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, categoryId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAttemptViews(resultSet);
            }
        } catch (SQLException exception) {
            LOGGER.error(
                    "Quiz attempt view query failed: operation=findTopByCategoryView categoryId={} limit={}",
                    categoryId,
                    limit,
                    exception
            );
            throw new IllegalStateException("Failed to load category leaderboard rows", exception);
        }
    }

    @Override
    public List<AttemptView> findRecentByUserView(long userId, int limit) {
        String sql = """
                SELECT qa.id, qa.user_id, u.username, qa.category_id, c.name AS category_name,
                       qa.score, qa.total_questions, qa.attempted_at
                FROM quiz_attempts qa
                JOIN users u ON u.id = qa.user_id
                LEFT JOIN categories c ON c.id = qa.category_id
                WHERE qa.user_id = ?
                ORDER BY qa.attempted_at DESC
                LIMIT ?
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAttemptViews(resultSet);
            }
        } catch (SQLException exception) {
            LOGGER.error(
                    "Quiz attempt view query failed: operation=findRecentByUserView userId={} limit={}",
                    userId,
                    limit,
                    exception
            );
            throw new IllegalStateException("Failed to load user history rows", exception);
        }
    }

    private List<QuizAttempt> mapAttempts(ResultSet resultSet) throws SQLException {
        List<QuizAttempt> attempts = new ArrayList<>();
        while (resultSet.next()) {
            long id = resultSet.getLong("id");
            long userId = resultSet.getLong("user_id");
            Long categoryId = resultSet.getObject("category_id") == null ? null : resultSet.getLong("category_id");
            int score = resultSet.getInt("score");
            int totalQuestions = resultSet.getInt("total_questions");
            Timestamp attemptedAtTimestamp = resultSet.getTimestamp("attempted_at");
            LocalDateTime attemptedAt = attemptedAtTimestamp.toLocalDateTime();
            attempts.add(new QuizAttempt(id, userId, categoryId, score, totalQuestions, attemptedAt));
        }
        return attempts;
    }

    private List<AttemptView> mapAttemptViews(ResultSet resultSet) throws SQLException {
        List<AttemptView> rows = new ArrayList<>();
        while (resultSet.next()) {
            long attemptId = resultSet.getLong("id");
            long userId = resultSet.getLong("user_id");
            String username = resultSet.getString("username");
            Long categoryId = resultSet.getObject("category_id") == null ? null : resultSet.getLong("category_id");
            String categoryName = resultSet.getString("category_name");
            int score = resultSet.getInt("score");
            int totalQuestions = resultSet.getInt("total_questions");
            Timestamp attemptedAtTimestamp = resultSet.getTimestamp("attempted_at");
            LocalDateTime attemptedAt = attemptedAtTimestamp.toLocalDateTime();
            rows.add(new AttemptView(
                    attemptId,
                    userId,
                    username,
                    categoryId,
                    categoryName,
                    score,
                    totalQuestions,
                    attemptedAt
            ));
        }
        return rows;
    }
}
