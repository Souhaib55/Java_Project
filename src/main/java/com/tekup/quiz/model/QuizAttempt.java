package com.tekup.quiz.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class QuizAttempt {
    private long id;
    private long userId;
    private Long categoryId;
    private int score;
    private int totalQuestions;
    private LocalDateTime attemptedAt;

    public QuizAttempt(long id,
                       long userId,
                       Long categoryId,
                       int score,
                       int totalQuestions,
                       LocalDateTime attemptedAt) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (totalQuestions <= 0) {
            throw new IllegalArgumentException("totalQuestions must be positive");
        }
        if (score < 0) {
            throw new IllegalArgumentException("score must be non-negative");
        }
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.attemptedAt = Objects.requireNonNull(attemptedAt, "attemptedAt must not be null");
    }

    public QuizAttempt(long userId, Long categoryId, int score, int totalQuestions, LocalDateTime attemptedAt) {
        this(0L, userId, categoryId, score, totalQuestions, attemptedAt);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public int getScore() {
        return score;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }
}
