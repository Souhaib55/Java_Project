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

class LeaderboardServiceTest {

    @Test
    void topGlobalRowsShouldUseDefaultTopSize() {
        RecordingQuizAttemptDao dao = new RecordingQuizAttemptDao();
        dao.globalRows = List.of(sampleRow("asma", "Science", 9));

        LeaderboardService service = new LeaderboardService(dao);
        List<AttemptView> rows = service.topGlobalRows();

        assertEquals(1, rows.size());
        assertEquals("asma", rows.get(0).getUsername());
        assertEquals(10, dao.lastGlobalLimit);
    }

    @Test
    void topByCategoryRowsShouldValidateCategoryId() {
        RecordingQuizAttemptDao dao = new RecordingQuizAttemptDao();
        LeaderboardService service = new LeaderboardService(dao);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.topByCategoryRows(0)
        );

        assertEquals("categoryId must be positive", exception.getMessage());
    }

    @Test
    void topByCategoryRowsShouldUseDefaultTopSize() {
        RecordingQuizAttemptDao dao = new RecordingQuizAttemptDao();
        dao.categoryRows = List.of(sampleRow("ali", "History", 7));

        LeaderboardService service = new LeaderboardService(dao);
        List<AttemptView> rows = service.topByCategoryRows(3L);

        assertEquals(1, rows.size());
        assertEquals("History", rows.get(0).getCategoryName());
        assertEquals(3L, dao.lastCategoryId);
        assertEquals(10, dao.lastCategoryLimit);
    }

    private AttemptView sampleRow(String username, String category, int score) {
        return new AttemptView(
                1L,
                10L,
                username,
                2L,
                category,
                score,
                10,
                LocalDateTime.now()
        );
    }

    private static class RecordingQuizAttemptDao implements QuizAttemptDao {
        private int lastGlobalLimit;
        private int lastCategoryLimit;
        private long lastCategoryId;
        private List<AttemptView> globalRows = new ArrayList<>();
        private List<AttemptView> categoryRows = new ArrayList<>();

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
            this.lastGlobalLimit = limit;
            return globalRows;
        }

        @Override
        public List<AttemptView> findTopByCategoryView(long categoryId, int limit) {
            this.lastCategoryId = categoryId;
            this.lastCategoryLimit = limit;
            return categoryRows;
        }

        @Override
        public List<AttemptView> findRecentByUserView(long userId, int limit) {
            return List.of();
        }
    }
}
