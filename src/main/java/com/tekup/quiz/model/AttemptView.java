package com.tekup.quiz.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class AttemptView {
    private final long attemptId;
    private final long userId;
    private final String username;
    private final Long categoryId;
    private final String categoryName;
    private final int score;
    private final int totalQuestions;
    private final LocalDateTime attemptedAt;

    public AttemptView(long attemptId,
                       long userId,
                       String username,
                       Long categoryId,
                       String categoryName,
                       int score,
                       int totalQuestions,
                       LocalDateTime attemptedAt) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (score < 0) {
            throw new IllegalArgumentException("score must be non-negative");
        }
        if (totalQuestions <= 0) {
            throw new IllegalArgumentException("totalQuestions must be positive");
        }
        this.attemptId = attemptId;
        this.userId = userId;
        this.username = validateText(username, "username");
        this.categoryId = categoryId;
        this.categoryName = categoryName == null || categoryName.isBlank() ? "Uncategorized" : categoryName.trim();
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.attemptedAt = Objects.requireNonNull(attemptedAt, "attemptedAt must not be null");
    }

    public long getAttemptId() {
        return attemptId;
    }

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
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

    private String validateText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
