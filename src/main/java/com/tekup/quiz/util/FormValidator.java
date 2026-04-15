package com.tekup.quiz.util;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FormValidator {
    private static final int MAX_CATEGORY_NAME_LENGTH = 100;
    private static final int MAX_PROMPT_LENGTH = 2000;
    private static final int MAX_OPTION_LENGTH = 255;

    private FormValidator() {
    }

    public static String validateCategoryName(String rawValue) {
        String normalized = requireNonBlank(rawValue, "Category name must not be blank");
        if (normalized.length() > MAX_CATEGORY_NAME_LENGTH) {
            throw new IllegalArgumentException("Category name must not exceed 100 characters");
        }
        return normalized;
    }

    public static String validatePrompt(String rawValue) {
        String normalized = requireNonBlank(rawValue, "Question prompt must not be blank");
        if (normalized.length() > MAX_PROMPT_LENGTH) {
            throw new IllegalArgumentException("Question prompt must not exceed 2000 characters");
        }
        return normalized;
    }

    public static String validateOption(String optionLabel, String rawValue) {
        String normalized = requireNonBlank(rawValue, optionLabel + " must not be blank");
        if (normalized.length() > MAX_OPTION_LENGTH) {
            throw new IllegalArgumentException(optionLabel + " must not exceed 255 characters");
        }
        return normalized;
    }

    public static void validateDistinctOptions(String optionA,
                                               String optionB,
                                               String optionC,
                                               String optionD) {
        List<String> options = List.of(optionA, optionB, optionC, optionD);
        Set<String> uniqueValues = new HashSet<>();
        for (String option : options) {
            String key = option.trim().toLowerCase(Locale.ROOT);
            if (!uniqueValues.add(key)) {
                throw new IllegalArgumentException("MCQ options must be unique");
            }
        }
    }

    public static String validateMcqCorrectAnswer(String rawValue) {
        String normalized = requireNonBlank(rawValue, "Correct answer must not be blank").toUpperCase(Locale.ROOT);
        if (!List.of("A", "B", "C", "D").contains(normalized)) {
            throw new IllegalArgumentException("For MCQ, correct answer must be A, B, C or D");
        }
        return normalized;
    }

    public static String validateTrueFalseCorrectAnswer(String rawValue) {
        String normalized = requireNonBlank(rawValue, "Correct answer must not be blank").toUpperCase(Locale.ROOT);
        if (!List.of("TRUE", "FALSE").contains(normalized)) {
            throw new IllegalArgumentException("For TRUE/FALSE, correct answer must be TRUE or FALSE");
        }
        return normalized;
    }

    private static String requireNonBlank(String rawValue, String message) {
        if (rawValue == null) {
            throw new IllegalArgumentException(message);
        }
        String normalized = rawValue.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }
}
