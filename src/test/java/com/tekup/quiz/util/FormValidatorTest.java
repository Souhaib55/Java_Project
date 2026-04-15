package com.tekup.quiz.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormValidatorTest {

    @Test
    void validateCategoryNameShouldRejectTooLongValues() {
        String tooLong = "a".repeat(101);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FormValidator.validateCategoryName(tooLong)
        );

        assertEquals("Category name must not exceed 100 characters", exception.getMessage());
    }

    @Test
    void validateDistinctOptionsShouldRejectDuplicatesIgnoringCase() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> FormValidator.validateDistinctOptions("A", "B", "a", "D")
        );

        assertEquals("MCQ options must be unique", exception.getMessage());
    }

    @Test
    void validateMcqCorrectAnswerShouldNormalizeCase() {
        String correct = FormValidator.validateMcqCorrectAnswer("  c ");

        assertEquals("C", correct);
    }

    @Test
    void validatePromptShouldTrimValue() {
        String prompt = FormValidator.validatePrompt("  What is Java?  ");

        assertEquals("What is Java?", prompt);
    }
}
