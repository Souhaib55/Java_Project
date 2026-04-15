package com.tekup.quiz.dao.jdbc;

import com.tekup.quiz.dao.QuestionDao;
import com.tekup.quiz.model.Difficulty;
import com.tekup.quiz.model.McqQuestion;
import com.tekup.quiz.model.Question;
import com.tekup.quiz.model.QuestionType;
import com.tekup.quiz.model.TrueFalseQuestion;
import com.tekup.quiz.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class JdbcQuestionDao implements QuestionDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcQuestionDao.class);

    @Override
    public Optional<Question> findById(long id) {
        String sql = "SELECT * FROM questions WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapQuestion(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            LOGGER.error("Failed to find question by id={}", id, exception);
            throw new IllegalStateException("Failed to find question by id", exception);
        }
    }

    @Override
    public List<Question> findAll() {
        String sql = "SELECT * FROM questions ORDER BY id DESC";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Question> questions = new ArrayList<>();
            while (resultSet.next()) {
                questions.add(mapQuestion(resultSet));
            }
            return questions;
        } catch (SQLException exception) {
            LOGGER.error("Failed to load all questions", exception);
            throw new IllegalStateException("Failed to load questions", exception);
        }
    }

    @Override
    public List<Question> findByCategory(long categoryId) {
        String sql = "SELECT * FROM questions WHERE category_id = ? ORDER BY id DESC";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, categoryId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Question> questions = new ArrayList<>();
                while (resultSet.next()) {
                    questions.add(mapQuestion(resultSet));
                }
                return questions;
            }
        } catch (SQLException exception) {
            LOGGER.error("Failed to load questions by categoryId={}", categoryId, exception);
            throw new IllegalStateException("Failed to load questions by category", exception);
        }
    }

    @Override
    public List<Question> findByCategoryAndDifficulty(long categoryId, Difficulty difficulty, int limit) {
        String sql = "SELECT * FROM questions WHERE category_id = ? AND difficulty = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, categoryId);
            statement.setString(2, difficulty.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Question> questions = new ArrayList<>();
                while (resultSet.next()) {
                    questions.add(mapQuestion(resultSet));
                }
                if (questions.isEmpty()) {
                    return questions;
                }

                Collections.shuffle(questions);
                if (questions.size() > limit) {
                    LOGGER.info(
                            "Question selection: categoryId={} difficulty={} requestedLimit={} matched={} returned={}",
                            categoryId,
                            difficulty,
                            limit,
                            questions.size(),
                            limit
                    );
                    return new ArrayList<>(questions.subList(0, limit));
                }

                LOGGER.info(
                        "Question selection: categoryId={} difficulty={} requestedLimit={} matched={} returned={}",
                        categoryId,
                        difficulty,
                        limit,
                        questions.size(),
                        questions.size()
                );
                return questions;
            }
        } catch (SQLException exception) {
            LOGGER.error(
                    "Failed to load quiz questions categoryId={} difficulty={} limit={}",
                    categoryId,
                    difficulty,
                    limit,
                    exception
            );
            throw new IllegalStateException("Failed to load quiz questions", exception);
        }
    }

    @Override
    public Question save(Question question) {
        if (question.getId() > 0) {
            return update(question);
        }
        return insert(question);
    }

    @Override
    public boolean deleteById(long id) {
        String sql = "DELETE FROM questions WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            LOGGER.error("Failed to delete question id={}", id, exception);
            throw new IllegalStateException("Failed to delete question", exception);
        }
    }

    private Question insert(Question question) {
        String sql = """
                INSERT INTO questions(type, category_id, difficulty, prompt, option_a, option_b, option_c, option_d, correct_answer)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindQuestion(statement, question);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    question.setId(keys.getLong(1));
                }
            }
            return question;
        } catch (SQLException exception) {
            throw mapSaveException(exception);
        }
    }

    private Question update(Question question) {
        String sql = """
                UPDATE questions
                SET type = ?, category_id = ?, difficulty = ?, prompt = ?, option_a = ?, option_b = ?, option_c = ?, option_d = ?, correct_answer = ?
                WHERE id = ?
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindQuestion(statement, question);
            statement.setLong(10, question.getId());
            statement.executeUpdate();
            return question;
        } catch (SQLException exception) {
            throw mapSaveException(exception);
        }
    }

    private void bindQuestion(PreparedStatement statement, Question question) throws SQLException {
        statement.setString(1, question.getType().name());
        statement.setLong(2, question.getCategoryId());
        statement.setString(3, question.getDifficulty().name());
        statement.setString(4, question.getPrompt());

        if (question instanceof McqQuestion mcqQuestion) {
            statement.setString(5, mcqQuestion.getOptionA());
            statement.setString(6, mcqQuestion.getOptionB());
            statement.setString(7, mcqQuestion.getOptionC());
            statement.setString(8, mcqQuestion.getOptionD());
        } else {
            statement.setString(5, "TRUE");
            statement.setString(6, "FALSE");
            statement.setString(7, null);
            statement.setString(8, null);
        }

        statement.setString(9, question.getCorrectAnswer());
    }

    private Question mapQuestion(ResultSet resultSet) throws SQLException {
        long id = resultSet.getLong("id");
        QuestionType type = QuestionType.valueOf(resultSet.getString("type"));
        long categoryId = resultSet.getLong("category_id");
        Difficulty difficulty = Difficulty.valueOf(resultSet.getString("difficulty"));
        String prompt = resultSet.getString("prompt");
        String correctAnswer = resultSet.getString("correct_answer");

        if (type == QuestionType.MCQ) {
            return new McqQuestion(
                    id,
                    categoryId,
                    difficulty,
                    prompt,
                    resultSet.getString("option_a"),
                    resultSet.getString("option_b"),
                    resultSet.getString("option_c"),
                    resultSet.getString("option_d"),
                    correctAnswer
            );
        }

        return new TrueFalseQuestion(id, categoryId, difficulty, prompt, correctAnswer);
    }

    private RuntimeException mapSaveException(SQLException exception) {
        if (isForeignKeyViolation(exception)) {
            return new IllegalArgumentException("Selected category does not exist");
        }
        return new IllegalStateException("Failed to save question", exception);
    }

    private boolean isForeignKeyViolation(SQLException exception) {
        String sqlState = exception.getSQLState();
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase(Locale.ROOT);
        return "23503".equals(sqlState)
                || exception.getErrorCode() == 1452
                || message.contains("foreign key")
                || message.contains("referential");
    }
}
