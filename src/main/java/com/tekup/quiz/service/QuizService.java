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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

        List<Question> difficultyMatched = new ArrayList<>(questionDao.findByCategoryAndDifficulty(categoryId, difficulty, questionCount));
        if (difficultyMatched.size() >= questionCount) {
            LOGGER.info(
                    "Quiz generation successful using exact difficulty match: categoryId={} difficulty={} returnedCount={}",
                    categoryId,
                    difficulty,
                    difficultyMatched.size()
            );
            return difficultyMatched;
        }

        List<Question> allCategoryQuestions = questionDao.findByCategory(categoryId);
        if (allCategoryQuestions.isEmpty()) {
            LOGGER.warn(
                    "Quiz generation rejected: no questions for categoryId={} difficulty={}",
                    categoryId,
                    difficulty
            );
            throw new IllegalStateException(
                    "No questions are available for the selected category"
            );
        }

        List<Question> merged = new ArrayList<>(difficultyMatched);
        Set<Long> selectedQuestionIds = new HashSet<>();
        for (Question question : merged) {
            selectedQuestionIds.add(question.getId());
        }

        List<Question> fallbackCandidates = new ArrayList<>();
        for (Question question : allCategoryQuestions) {
            if (!selectedQuestionIds.contains(question.getId())) {
                fallbackCandidates.add(question);
            }
        }
        Collections.shuffle(fallbackCandidates);

        int missingCount = Math.max(0, questionCount - merged.size());
        for (int index = 0; index < missingCount && index < fallbackCandidates.size(); index++) {
            merged.add(fallbackCandidates.get(index));
        }

        Collections.shuffle(merged);

        LOGGER.info(
                "Quiz generation successful: categoryId={} difficulty={} requestedCount={} matchedDifficultyCount={} returnedCount={}",
                categoryId,
                difficulty,
                questionCount,
                difficultyMatched.size(),
                merged.size()
        );
        return merged;
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
