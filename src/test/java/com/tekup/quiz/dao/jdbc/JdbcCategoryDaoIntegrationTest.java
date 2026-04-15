package com.tekup.quiz.dao.jdbc;

import com.tekup.quiz.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcCategoryDaoIntegrationTest {

    private JdbcCategoryDao categoryDao;

    @BeforeEach
    void setUp() {
        IntegrationTestDatabase.resetSchema();
        categoryDao = new JdbcCategoryDao();
    }

    @Test
    void findAllShouldBeSortedByName() {
        categoryDao.save(new Category("Zoology"));
        categoryDao.save(new Category("Art"));

        List<Category> categories = categoryDao.findAll();

        assertEquals(2, categories.size());
        assertEquals("Art", categories.get(0).getName());
        assertEquals("Zoology", categories.get(1).getName());
    }

    @Test
    void saveShouldRejectDuplicateCategoryName() {
        categoryDao.save(new Category("Science"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryDao.save(new Category("Science"))
        );

        assertEquals("Category name already exists", exception.getMessage());
    }

    @Test
    void isInUseShouldReturnTrueWhenQuestionLinked() {
        long categoryId = IntegrationTestDatabase.insertCategory("Science");
        IntegrationTestDatabase.insertQuestionMcq(categoryId, "EASY", "Q1", "A");

        assertTrue(categoryDao.isInUse(categoryId));
    }

    @Test
    void deleteByIdShouldRemoveUnusedCategory() {
        Category category = categoryDao.save(new Category("Programming"));

        boolean deleted = categoryDao.deleteById(category.getId());

        assertTrue(deleted);
        assertTrue(categoryDao.findById(category.getId()).isEmpty());
    }

    @Test
    void deleteByIdShouldRejectCategoryWithLinkedQuestions() {
        long categoryId = IntegrationTestDatabase.insertCategory("History");
        IntegrationTestDatabase.insertQuestionTrueFalse(categoryId, "MEDIUM", "History prompt", "TRUE");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> categoryDao.deleteById(categoryId)
        );

        assertEquals("Cannot delete category because it has linked questions", exception.getMessage());
    }

    @Test
    void deleteByIdShouldReturnFalseWhenCategoryMissing() {
        assertFalse(categoryDao.deleteById(9999));
    }
}
