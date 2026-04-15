package com.tekup.quiz.model;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public abstract class Question {
    private long id;
    private final QuestionType type;
    private long categoryId;
    private Difficulty difficulty;
    private String prompt;
    private String correctAnswer;

    protected Question(long id,
                       QuestionType type,
                       long categoryId,
                       Difficulty difficulty,
                       String prompt,
                       String correctAnswer) {
        this.id = id;
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.categoryId = categoryId;
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty must not be null");
        this.prompt = validatePrompt(prompt);
        this.correctAnswer = normalizeAnswer(correctAnswer);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public QuestionType getType() {
        return type;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty must not be null");
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = validatePrompt(prompt);
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = normalizeAnswer(correctAnswer);
    }

    public int weight() {
        return difficulty.weight();
    }

    public boolean isCorrect(String answer) {
        return correctAnswer.equals(normalizeAnswer(answer));
    }

    protected String normalizeAnswer(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Answer must not be blank");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String validatePrompt(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Question prompt must not be blank");
        }
        return value.trim();
    }

    public abstract List<String> getOptions();
}
