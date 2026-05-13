package com.tekup.quiz.service;

import com.tekup.quiz.dao.QuestionDao;
import com.tekup.quiz.dao.QuizAttemptDao;
import com.tekup.quiz.model.AttemptView;
import com.tekup.quiz.model.Difficulty;
import com.tekup.quiz.model.McqQuestion;
import com.tekup.quiz.model.Question;
import com.tekup.quiz.model.QuizAttempt;
import com.tekup.quiz.model.QuizResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuizServiceTest {

    @Test
    void evaluateShouldApplyWeightedScoring() {
        QuestionDao questionDao = new FakeQuestionDao();
        RecordingAttemptDao attemptDao = new RecordingAttemptDao();
        QuizService quizService = new QuizService(questionDao, attemptDao);

        List<Question> questions = List.of(
                new McqQuestion(1L, Difficulty.EASY, "Q1", "A1", "A2", "A3", "A4", "A"),
                new McqQuestion(1L, Difficulty.HARD, "Q2", "B1", "B2", "B3", "B4", "C")
        );
        List<String> answers = List.of("A", "C");

        QuizResult result = quizService.evaluate(10L, 1L, questions, answers);

        assertEquals(4, result.getScore());
        assertEquals(2, result.getTotalQuestions());
        assertEquals(1, attemptDao.savedAttempts.size());
        assertEquals(4, attemptDao.savedAttempts.get(0).getScore());
    }

    @Test
    void evaluateShouldTreatBlankAndInvalidAnswersAsIncorrect() {
        QuestionDao questionDao = new FakeQuestionDao();
        RecordingAttemptDao attemptDao = new RecordingAttemptDao();
        QuizService quizService = new QuizService(questionDao, attemptDao);

        List<Question> questions = List.of(
                new McqQuestion(1L, Difficulty.EASY, "Q1", "A1", "A2", "A3", "A4", "A"),
                new McqQuestion(1L, Difficulty.MEDIUM, "Q2", "B1", "B2", "B3", "B4", "B"),
                new McqQuestion(1L, Difficulty.HARD, "Q3", "C1", "C2", "C3", "C4", "D")
        );
        List<String> answers = List.of("", "Z", "D");

        QuizResult result = quizService.evaluate(10L, 1L, questions, answers);

        assertEquals(3, result.getScore());
        assertEquals(1, attemptDao.savedAttempts.size());
    }

    @Test
    void generateQuizShouldRejectNonPositiveQuestionCount() {
        QuestionDao questionDao = new FakeQuestionDao();
        RecordingAttemptDao attemptDao = new RecordingAttemptDao();
        QuizService quizService = new QuizService(questionDao, attemptDao);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> quizService.generateQuiz(1L, Difficulty.EASY, 0)
        );

        assertEquals("questionCount must be positive", exception.getMessage());
    }

    @Test
    void generateQuizShouldRejectNonPositiveCategoryId() {
        QuestionDao questionDao = new FakeQuestionDao();
        RecordingAttemptDao attemptDao = new RecordingAttemptDao();
        QuizService quizService = new QuizService(questionDao, attemptDao);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> quizService.generateQuiz(0, Difficulty.EASY, 5)
        );

        assertEquals("categoryId must be positive", exception.getMessage());
    }

    @Test
    void generateQuizShouldRejectNullDifficulty() {
        QuestionDao questionDao = new FakeQuestionDao();
        RecordingAttemptDao attemptDao = new RecordingAttemptDao();
        QuizService quizService = new QuizService(questionDao, attemptDao);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> quizService.generateQuiz(1L, null, 5)
        );

        assertEquals("difficulty must not be null", exception.getMessage());
    }

    @Test
    void generateQuizShouldRejectWhenNoQuestionsAvailable() {
        QuestionDao questionDao = new FakeQuestionDao();
        RecordingAttemptDao attemptDao = new RecordingAttemptDao();
        QuizService quizService = new QuizService(questionDao, attemptDao);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> quizService.generateQuiz(1L, Difficulty.EASY, 5)
        );

        assertNotNull(exception.getMessage());
        assertEquals("No questions are available for the selected category", exception.getMessage());
    }

    @Test
    void generateQuizShouldFallbackToOtherDifficultiesWhenSelectedDifficultyHasNoMatch() {
        List<Question> categoryQuestions = List.of(
                new McqQuestion(1L, 5L, Difficulty.HARD, "Q1", "A", "B", "C", "D", "A"),
                new McqQuestion(2L, 5L, Difficulty.MEDIUM, "Q2", "A", "B", "C", "D", "B")
        );
        QuestionDao questionDao = new FakeQuestionDao(categoryQuestions, List.of());
        RecordingAttemptDao attemptDao = new RecordingAttemptDao();
        QuizService quizService = new QuizService(questionDao, attemptDao);

        List<Question> questions = quizService.generateQuiz(5L, Difficulty.EASY, 1);

        assertEquals(1, questions.size());
        assertEquals(5L, questions.get(0).getCategoryId());
    }

    @Test
    void generateQuizShouldTopUpWhenDifficultyMatchesAreInsufficient() {
        List<Question> difficultyMatches = List.of(
                new McqQuestion(10L, 7L, Difficulty.MEDIUM, "Q1", "A", "B", "C", "D", "A")
        );
        List<Question> categoryQuestions = List.of(
                difficultyMatches.get(0),
                new McqQuestion(11L, 7L, Difficulty.EASY, "Q2", "A", "B", "C", "D", "B"),
                new McqQuestion(12L, 7L, Difficulty.HARD, "Q3", "A", "B", "C", "D", "C")
        );
        QuestionDao questionDao = new FakeQuestionDao(categoryQuestions, difficultyMatches);
        RecordingAttemptDao attemptDao = new RecordingAttemptDao();
        QuizService quizService = new QuizService(questionDao, attemptDao);

        List<Question> questions = quizService.generateQuiz(7L, Difficulty.MEDIUM, 3);

        assertEquals(3, questions.size());
        assertEquals(3, questions.stream().map(Question::getId).distinct().count());
    }

    @Test
    void evaluateShouldRejectMismatchedQuestionAndAnswerCount() {
        QuestionDao questionDao = new FakeQuestionDao();
        RecordingAttemptDao attemptDao = new RecordingAttemptDao();
        QuizService quizService = new QuizService(questionDao, attemptDao);

        List<Question> questions = List.of(
                new McqQuestion(1L, Difficulty.EASY, "Q1", "A", "B", "C", "D", "A"),
                new McqQuestion(1L, Difficulty.MEDIUM, "Q2", "A", "B", "C", "D", "B")
        );
        List<String> answers = List.of("A");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> quizService.evaluate(1L, 1L, questions, answers)
        );

        assertEquals("Each question must have one submitted answer", exception.getMessage());
    }

    @Test
    void evaluateShouldRejectNonPositiveUserId() {
        QuestionDao questionDao = new FakeQuestionDao();
        RecordingAttemptDao attemptDao = new RecordingAttemptDao();
        QuizService quizService = new QuizService(questionDao, attemptDao);

        List<Question> questions = List.of(
                new McqQuestion(1L, Difficulty.EASY, "Q1", "A", "B", "C", "D", "A")
        );
        List<String> answers = List.of("A");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> quizService.evaluate(0, 1L, questions, answers)
        );

        assertEquals("userId must be positive", exception.getMessage());
    }

    private static class FakeQuestionDao implements QuestionDao {
        private final List<Question> categoryQuestions;
        private final List<Question> difficultyQuestions;

        private FakeQuestionDao() {
            this(List.of(), List.of());
        }

        private FakeQuestionDao(List<Question> categoryQuestions, List<Question> difficultyQuestions) {
            this.categoryQuestions = categoryQuestions;
            this.difficultyQuestions = difficultyQuestions;
        }

        @Override
        public Optional<Question> findById(long id) {
            return Optional.empty();
        }

        @Override
        public List<Question> findAll() {
            return List.of();
        }

        @Override
        public List<Question> findByCategory(long categoryId) {
            return categoryQuestions;
        }

        @Override
        public List<Question> findByCategoryAndDifficulty(long categoryId, Difficulty difficulty, int limit) {
            if (difficultyQuestions.size() <= limit) {
                return difficultyQuestions;
            }
            return difficultyQuestions.subList(0, limit);
        }

        @Override
        public Question save(Question question) {
            return question;
        }

        @Override
        public boolean deleteById(long id) {
            return false;
        }
    }

    private static class RecordingAttemptDao implements QuizAttemptDao {
        private final List<QuizAttempt> savedAttempts = new ArrayList<>();

        @Override
        public QuizAttempt save(QuizAttempt attempt) {
            if (attempt.getId() == 0) {
                attempt.setId(savedAttempts.size() + 1L);
            }
            savedAttempts.add(attempt);
            return attempt;
        }

        @Override
        public List<QuizAttempt> findByUser(long userId) {
            return List.of();
        }

        @Override
        public List<QuizAttempt> findTopGlobal(int limit) {
            return List.of();
        }

        @Override
        public List<QuizAttempt> findTopByCategory(long categoryId, int limit) {
            return List.of();
        }

        @Override
        public List<AttemptView> findTopGlobalView(int limit) {
            return List.of();
        }

        @Override
        public List<AttemptView> findTopByCategoryView(long categoryId, int limit) {
            return List.of();
        }

        @Override
        public List<AttemptView> findRecentByUserView(long userId, int limit) {
            return List.of();
        }
    }
}
