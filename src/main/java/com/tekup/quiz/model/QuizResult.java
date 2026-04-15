package com.tekup.quiz.model;

import java.util.List;
import java.util.Objects;

public class QuizResult {
    private final long userId;
    private final long categoryId;
    private final int score;
    private final int totalQuestions;
    private final List<Question> questions;

    public QuizResult(long userId, long categoryId, int score, int totalQuestions, List<Question> questions) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (categoryId <= 0) {
            throw new IllegalArgumentException("categoryId must be positive");
        }
        if (score < 0) {
            throw new IllegalArgumentException("score must be non-negative");
        }
        if (totalQuestions <= 0) {
            throw new IllegalArgumentException("totalQuestions must be positive");
        }
        this.userId = userId;
        this.categoryId = categoryId;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.questions = List.copyOf(Objects.requireNonNull(questions, "questions must not be null"));
    }

    public long getUserId() {
        return userId;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public int getScore() {
        return score;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public List<Question> getQuestions() {
        return questions;
    }
}
