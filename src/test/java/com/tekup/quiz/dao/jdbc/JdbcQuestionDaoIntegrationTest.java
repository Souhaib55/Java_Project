package com.tekup.quiz.dao.jdbc;

import com.tekup.quiz.model.Difficulty;
import com.tekup.quiz.model.McqQuestion;
import com.tekup.quiz.model.Question;
import com.tekup.quiz.model.TrueFalseQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcQuestionDaoIntegrationTest {

    private JdbcQuestionDao questionDao;

    @BeforeEach
    void setUp() {
        IntegrationTestDatabase.resetSchema();
        questionDao = new JdbcQuestionDao();
    }

    @Test
    void saveShouldInsertAndFindMcqQuestion() {
        long categoryId = IntegrationTestDatabase.insertCategory("Science");

        Question saved = questionDao.save(new McqQuestion(
                categoryId,
                Difficulty.EASY,
                "What is H2O?",
                "Water",
                "Fire",
                "Air",
                "Earth",
                "A"
        ));

        Question loaded = questionDao.findById(saved.getId()).orElseThrow();
        assertInstanceOf(McqQuestion.class, loaded);
        assertEquals(Difficulty.EASY, loaded.getDifficulty());
        assertEquals("A", loaded.getCorrectAnswer());
    }

    @Test
    void saveShouldInsertAndFindTrueFalseQuestion() {
        long categoryId = IntegrationTestDatabase.insertCategory("History");

        Question saved = questionDao.save(new TrueFalseQuestion(
                categoryId,
                Difficulty.MEDIUM,
                "The Roman Empire existed.",
                "TRUE"
        ));

        Question loaded = questionDao.findById(saved.getId()).orElseThrow();
        assertInstanceOf(TrueFalseQuestion.class, loaded);
        assertEquals("TRUE", loaded.getCorrectAnswer());
    }

    @Test
    void findByCategoryShouldFilterRows() {
        long catScience = IntegrationTestDatabase.insertCategory("Science");
        long catHistory = IntegrationTestDatabase.insertCategory("History");

        questionDao.save(new McqQuestion(catScience, Difficulty.EASY, "S1", "A", "B", "C", "D", "A"));
        questionDao.save(new McqQuestion(catScience, Difficulty.HARD, "S2", "A", "B", "C", "D", "B"));
        questionDao.save(new McqQuestion(catHistory, Difficulty.EASY, "H1", "A", "B", "C", "D", "C"));

        List<Question> scienceQuestions = questionDao.findByCategory(catScience);

        assertEquals(2, scienceQuestions.size());
        assertTrue(scienceQuestions.stream().allMatch(q -> q.getCategoryId() == catScience));
    }

    @Test
    void findByCategoryAndDifficultyShouldRespectLimit() {
        long categoryId = IntegrationTestDatabase.insertCategory("Programming");

        questionDao.save(new McqQuestion(categoryId, Difficulty.EASY, "Q1", "A", "B", "C", "D", "A"));
        questionDao.save(new McqQuestion(categoryId, Difficulty.EASY, "Q2", "A", "B", "C", "D", "B"));
        questionDao.save(new McqQuestion(categoryId, Difficulty.EASY, "Q3", "A", "B", "C", "D", "C"));
        questionDao.save(new McqQuestion(categoryId, Difficulty.HARD, "Q4", "A", "B", "C", "D", "D"));

        List<Question> easyQuestions = questionDao.findByCategoryAndDifficulty(categoryId, Difficulty.EASY, 2);

        assertEquals(2, easyQuestions.size());
        assertTrue(easyQuestions.stream().allMatch(q -> q.getDifficulty() == Difficulty.EASY));
    }

    @Test
    void deleteByIdShouldRemoveQuestion() {
        long categoryId = IntegrationTestDatabase.insertCategory("Math");
        Question saved = questionDao.save(new McqQuestion(categoryId, Difficulty.MEDIUM, "Q", "A", "B", "C", "D", "A"));

        boolean deleted = questionDao.deleteById(saved.getId());

        assertTrue(deleted);
        assertTrue(questionDao.findById(saved.getId()).isEmpty());
    }

    @Test
    void saveShouldRejectUnknownCategory() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> questionDao.save(new McqQuestion(99999L, Difficulty.EASY, "Q", "A", "B", "C", "D", "A"))
        );

        assertEquals("Selected category does not exist", exception.getMessage());
    }

    @Test
    void deleteByIdShouldReturnFalseForMissingQuestion() {
        assertFalse(questionDao.deleteById(424242));
    }
}
