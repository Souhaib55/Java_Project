package com.tekup.quiz.model;

import java.util.List;
import java.util.Locale;

public class TrueFalseQuestion extends Question {
    private static final List<String> OPTIONS = List.of("TRUE", "FALSE");

    public TrueFalseQuestion(long id,
                             long categoryId,
                             Difficulty difficulty,
                             String prompt,
                             String correctAnswer) {
        super(id, QuestionType.TRUE_FALSE, categoryId, difficulty, prompt, normalizeTrueFalseAnswer(correctAnswer));
    }

    public TrueFalseQuestion(long categoryId,
                             Difficulty difficulty,
                             String prompt,
                             String correctAnswer) {
        this(0L, categoryId, difficulty, prompt, correctAnswer);
    }

    @Override
    public void setCorrectAnswer(String correctAnswer) {
        super.setCorrectAnswer(normalizeTrueFalseAnswer(correctAnswer));
    }

    @Override
    public List<String> getOptions() {
        return OPTIONS;
    }

    private static String normalizeTrueFalseAnswer(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("True/False answer must not be blank");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!"TRUE".equals(normalized) && !"FALSE".equals(normalized)) {
            throw new IllegalArgumentException("True/False answer must be TRUE or FALSE");
        }
        return normalized;
    }
}
