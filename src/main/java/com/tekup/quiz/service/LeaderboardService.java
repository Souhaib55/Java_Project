package com.tekup.quiz.service;

import com.tekup.quiz.model.AttemptView;
import com.tekup.quiz.dao.QuizAttemptDao;
import com.tekup.quiz.model.QuizAttempt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class LeaderboardService {
    private static final int DEFAULT_TOP_SIZE = 10;
    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderboardService.class);

    private final QuizAttemptDao quizAttemptDao;

    public LeaderboardService(QuizAttemptDao quizAttemptDao) {
        this.quizAttemptDao = Objects.requireNonNull(quizAttemptDao, "quizAttemptDao must not be null");
    }

    public List<QuizAttempt> topGlobal() {
        LOGGER.info("Leaderboard requested: scope=global limit={}", DEFAULT_TOP_SIZE);
        try {
            List<QuizAttempt> rows = quizAttemptDao.findTopGlobal(DEFAULT_TOP_SIZE);
            LOGGER.info("Leaderboard loaded: scope=global rowCount={}", rows.size());
            return rows;
        } catch (RuntimeException exception) {
            LOGGER.error("Leaderboard load failed: scope=global limit={}", DEFAULT_TOP_SIZE, exception);
            throw exception;
        }
    }

    public List<QuizAttempt> topByCategory(long categoryId) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("categoryId must be positive");
        }

        LOGGER.info("Leaderboard requested: scope=category categoryId={} limit={}", categoryId, DEFAULT_TOP_SIZE);
        try {
            List<QuizAttempt> rows = quizAttemptDao.findTopByCategory(categoryId, DEFAULT_TOP_SIZE);
            LOGGER.info("Leaderboard loaded: scope=category categoryId={} rowCount={}", categoryId, rows.size());
            return rows;
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Leaderboard load failed: scope=category categoryId={} limit={}",
                    categoryId,
                    DEFAULT_TOP_SIZE,
                    exception
            );
            throw exception;
        }
    }

    public List<AttemptView> topGlobalRows() {
        LOGGER.info("Leaderboard rows requested: scope=global limit={}", DEFAULT_TOP_SIZE);
        try {
            List<AttemptView> rows = quizAttemptDao.findTopGlobalView(DEFAULT_TOP_SIZE);
            LOGGER.info("Leaderboard rows loaded: scope=global rowCount={}", rows.size());
            return rows;
        } catch (RuntimeException exception) {
            LOGGER.error("Leaderboard rows load failed: scope=global limit={}", DEFAULT_TOP_SIZE, exception);
            throw exception;
        }
    }

    public List<AttemptView> topByCategoryRows(long categoryId) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("categoryId must be positive");
        }

        LOGGER.info("Leaderboard rows requested: scope=category categoryId={} limit={}", categoryId, DEFAULT_TOP_SIZE);
        try {
            List<AttemptView> rows = quizAttemptDao.findTopByCategoryView(categoryId, DEFAULT_TOP_SIZE);
            LOGGER.info("Leaderboard rows loaded: scope=category categoryId={} rowCount={}", categoryId, rows.size());
            return rows;
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Leaderboard rows load failed: scope=category categoryId={} limit={}",
                    categoryId,
                    DEFAULT_TOP_SIZE,
                    exception
            );
            throw exception;
        }
    }
}
