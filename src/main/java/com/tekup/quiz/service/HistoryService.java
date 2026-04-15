package com.tekup.quiz.service;

import com.tekup.quiz.dao.QuizAttemptDao;
import com.tekup.quiz.model.AttemptView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class HistoryService {
    private static final int DEFAULT_HISTORY_LIMIT = 20;
    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryService.class);

    private final QuizAttemptDao quizAttemptDao;

    public HistoryService(QuizAttemptDao quizAttemptDao) {
        this.quizAttemptDao = Objects.requireNonNull(quizAttemptDao, "quizAttemptDao must not be null");
    }

    public List<AttemptView> recentForUser(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }

        LOGGER.info("History requested: userId={} limit={}", userId, DEFAULT_HISTORY_LIMIT);
        try {
            List<AttemptView> rows = quizAttemptDao.findRecentByUserView(userId, DEFAULT_HISTORY_LIMIT);
            LOGGER.info("History loaded: userId={} rowCount={}", userId, rows.size());
            return rows;
        } catch (RuntimeException exception) {
            LOGGER.error("History load failed: userId={} limit={}", userId, DEFAULT_HISTORY_LIMIT, exception);
            throw exception;
        }
    }
}
