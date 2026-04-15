package com.tekup.quiz.service;

import com.tekup.quiz.dao.QuizAttemptDao;
import com.tekup.quiz.model.AttemptView;
import com.tekup.quiz.model.QuizAttempt;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HistoryServiceTest {

    @Test
    void recentForUserShouldValidateUserId() {
        RecordingQuizAttemptDao dao = new RecordingQuizAttemptDao();
        HistoryService historyService = new HistoryService(dao);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> historyService.recentForUser(0)
        );

        assertEquals("userId must be positive", exception.getMessage());
    }

    @Test
    void recentForUserShouldUseDefaultLimit() {
        RecordingQuizAttemptDao dao = new RecordingQuizAttemptDao();
        dao.historyRows = List.of(new AttemptView(
                2L,
                10L,
                "asma",
                1L,
                "Science",
                8,
                10,
                LocalDateTime.now()
        ));
        HistoryService historyService = new HistoryService(dao);

        List<AttemptView> rows = historyService.recentForUser(10L);

        assertEquals(1, rows.size());
        assertEquals("asma", rows.get(0).getUsername());
        assertEquals(10L, dao.lastHistoryUserId);
        assertEquals(20, dao.lastHistoryLimit);
    }

    private static class RecordingQuizAttemptDao implements QuizAttemptDao {
        private long lastHistoryUserId;
        private int lastHistoryLimit;
        private List<AttemptView> historyRows = new ArrayList<>();

        @Override
        public QuizAttempt save(QuizAttempt attempt) {
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
            this.lastHistoryUserId = userId;
            this.lastHistoryLimit = limit;
            return historyRows;
        }
    }
}
