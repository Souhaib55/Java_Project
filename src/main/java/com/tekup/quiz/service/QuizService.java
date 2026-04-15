package com.tekup.quiz.service;

import com.tekup.quiz.dao.QuestionDao;
import com.tekup.quiz.dao.QuizAttemptDao;
import com.tekup.quiz.model.Difficulty;
import com.tekup.quiz.model.Question;
import com.tekup.quiz.model.QuizAttempt;
import com.tekup.quiz.model.QuizResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class QuizService {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuizService.class);

    private final QuestionDao questionDao;
    private final QuizAttemptDao quizAttemptDao;

    public QuizService(QuestionDao questionDao, QuizAttemptDao quizAttemptDao) {
        this.questionDao = Objects.requireNonNull(questionDao, "questionDao must not be null");
        this.quizAttemptDao = Objects.requireNonNull(quizAttemptDao, "quizAttemptDao must not be null");
    }

    public List<Question> generateQuiz(long categoryId, Difficulty difficulty, int questionCount) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("categoryId must be positive");
        }
        Objects.requireNonNull(difficulty, "difficulty must not be null");
        if (questionCount <= 0) {
            throw new IllegalArgumentException("questionCount must be positive");
        }

        LOGGER.info(
                "Quiz generation requested: categoryId={} difficulty={} requestedCount={}",
                categoryId,
                difficulty,
                questionCount
        );

        List<Question> questions = questionDao.findByCategoryAndDifficulty(categoryId, difficulty, questionCount);
        if (questions.isEmpty()) {
            LOGGER.warn(
                    "Quiz generation rejected: no questions for categoryId={} difficulty={}",
                    categoryId,
                    difficulty
            );
            throw new IllegalStateException(
                    "No questions are available for the selected category and difficulty"
            );
        }

        LOGGER.info(
                "Quiz generation successful: categoryId={} difficulty={} returnedCount={}",
                categoryId,
                difficulty,
                questions.size()
        );
        return questions;
    }

    public QuizResult evaluate(long userId,
                               long categoryId,
                               List<Question> questions,
                               List<String> submittedAnswers) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (categoryId <= 0) {
            throw new IllegalArgumentException("categoryId must be positive");
        }
        Objects.requireNonNull(questions, "questions must not be null");
        Objects.requireNonNull(submittedAnswers, "submittedAnswers must not be null");

        if (questions.size() != submittedAnswers.size()) {
            throw new IllegalArgumentException("Each question must have one submitted answer");
        }

        int totalScore = 0;
        for (int index = 0; index < questions.size(); index++) {
            Question question = questions.get(index);
            String answer = submittedAnswers.get(index);
            if (isCorrectSubmission(question, answer)) {
                totalScore += question.weight();
            }
        }

        QuizAttempt attempt = new QuizAttempt(
                userId,
                categoryId,
                totalScore,
                questions.size(),
                LocalDateTime.now()
        );
        quizAttemptDao.save(attempt);

        LOGGER.info(
            "Quiz evaluated: userId={} categoryId={} score={} totalQuestions={}",
            userId,
            categoryId,
            totalScore,
            questions.size()
        );

        return new QuizResult(userId, categoryId, totalScore, questions.size(), questions);
    }

    private boolean isCorrectSubmission(Question question, String answer) {
        if (answer == null || answer.isBlank()) {
            return false;
        }
        try {
            return question.isCorrect(answer);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
