package com.tekup.quiz.dao.jdbc;

import com.tekup.quiz.model.AttemptView;
import com.tekup.quiz.model.QuizAttempt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcQuizAttemptDaoIntegrationTest {

    private JdbcQuizAttemptDao attemptDao;

    @BeforeEach
    void setUp() {
        IntegrationTestDatabase.resetSchema();
        attemptDao = new JdbcQuizAttemptDao();
    }

    @Test
    void saveAndFindByUserShouldReturnLatestFirst() {
        long userId = IntegrationTestDatabase.insertUser("asma", "PLAYER");
        long categoryId = IntegrationTestDatabase.insertCategory("Science");

        attemptDao.save(new QuizAttempt(userId, categoryId, 3, 5, LocalDateTime.of(2026, 1, 1, 10, 0)));
        attemptDao.save(new QuizAttempt(userId, categoryId, 4, 5, LocalDateTime.of(2026, 1, 1, 11, 0)));

        List<QuizAttempt> attempts = attemptDao.findByUser(userId);

        assertEquals(2, attempts.size());
        assertEquals(4, attempts.get(0).getScore());
        assertEquals(3, attempts.get(1).getScore());
    }

    @Test
    void topGlobalViewShouldJoinUsernameAndCategory() {
        long userAsma = IntegrationTestDatabase.insertUser("asma", "PLAYER");
        long userAli = IntegrationTestDatabase.insertUser("ali", "PLAYER");
        long catScience = IntegrationTestDatabase.insertCategory("Science");

        IntegrationTestDatabase.insertAttempt(userAsma, catScience, 7, 10, LocalDateTime.of(2026, 1, 1, 10, 0));
        IntegrationTestDatabase.insertAttempt(userAli, catScience, 9, 10, LocalDateTime.of(2026, 1, 1, 9, 0));

        List<AttemptView> rows = attemptDao.findTopGlobalView(10);

        assertEquals(2, rows.size());
        assertEquals("ali", rows.get(0).getUsername());
        assertEquals("Science", rows.get(0).getCategoryName());
    }

    @Test
    void topByCategoryViewShouldFilterCategory() {
        long userId = IntegrationTestDatabase.insertUser("asma", "PLAYER");
        long catScience = IntegrationTestDatabase.insertCategory("Science");
        long catHistory = IntegrationTestDatabase.insertCategory("History");

        IntegrationTestDatabase.insertAttempt(userId, catScience, 8, 10, LocalDateTime.now());
        IntegrationTestDatabase.insertAttempt(userId, catHistory, 10, 10, LocalDateTime.now());

        List<AttemptView> rows = attemptDao.findTopByCategoryView(catScience, 10);

        assertEquals(1, rows.size());
        assertEquals("Science", rows.get(0).getCategoryName());
    }

    @Test
    void recentByUserViewShouldRespectLimit() {
        long userId = IntegrationTestDatabase.insertUser("asma", "PLAYER");
        long categoryId = IntegrationTestDatabase.insertCategory("Programming");

        IntegrationTestDatabase.insertAttempt(userId, categoryId, 1, 5, LocalDateTime.of(2026, 1, 1, 8, 0));
        IntegrationTestDatabase.insertAttempt(userId, categoryId, 2, 5, LocalDateTime.of(2026, 1, 1, 9, 0));
        IntegrationTestDatabase.insertAttempt(userId, categoryId, 3, 5, LocalDateTime.of(2026, 1, 1, 10, 0));

        List<AttemptView> rows = attemptDao.findRecentByUserView(userId, 2);

        assertEquals(2, rows.size());
        assertTrue(rows.get(0).getAttemptedAt().isAfter(rows.get(1).getAttemptedAt()));
    }
}
