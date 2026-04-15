package com.tekup.quiz.ui;

import com.tekup.quiz.model.Category;
import com.tekup.quiz.model.Difficulty;
import com.tekup.quiz.model.QuizResult;
import com.tekup.quiz.model.User;
import com.tekup.quiz.util.AppConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

public class SessionContext {
    private static final long DEFAULT_SESSION_TIMEOUT_MINUTES = 15;

    private final Duration sessionTimeout;
    private final Supplier<Instant> nowProvider;

    private User currentUser;
    private Category selectedCategory;
    private Difficulty selectedDifficulty;
    private Integer selectedQuestionCount;
    private QuizResult lastQuizResult;
    private Instant lastActivityAt;
    private boolean sessionExpiredNotice;

    public SessionContext() {
        this(Duration.ofMinutes(readSessionTimeoutMinutes()), Instant::now);
    }

    SessionContext(Duration sessionTimeout, Supplier<Instant> nowProvider) {
        this.sessionTimeout = Objects.requireNonNull(sessionTimeout, "sessionTimeout must not be null");
        this.nowProvider = Objects.requireNonNull(nowProvider, "nowProvider must not be null");
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        if (currentUser == null) {
            this.lastActivityAt = null;
        } else {
            markActivity();
        }
        this.sessionExpiredNotice = false;
    }

    public Category getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(Category selectedCategory) {
        this.selectedCategory = selectedCategory;
        markActivityIfAuthenticated();
    }

    public Difficulty getSelectedDifficulty() {
        return selectedDifficulty;
    }

    public void setSelectedDifficulty(Difficulty selectedDifficulty) {
        this.selectedDifficulty = selectedDifficulty;
        markActivityIfAuthenticated();
    }

    public Integer getSelectedQuestionCount() {
        return selectedQuestionCount;
    }

    public void setSelectedQuestionCount(Integer selectedQuestionCount) {
        this.selectedQuestionCount = selectedQuestionCount;
        markActivityIfAuthenticated();
    }

    public QuizResult getLastQuizResult() {
        return lastQuizResult;
    }

    public void setLastQuizResult(QuizResult lastQuizResult) {
        this.lastQuizResult = lastQuizResult;
        markActivityIfAuthenticated();
    }

    public boolean isSessionExpired() {
        if (currentUser == null || lastActivityAt == null) {
            return false;
        }
        return nowProvider.get().isAfter(lastActivityAt.plus(sessionTimeout));
    }

    public void markActivity() {
        if (currentUser != null) {
            lastActivityAt = nowProvider.get();
        }
    }

    public void expireSession() {
        this.currentUser = null;
        this.selectedCategory = null;
        this.selectedDifficulty = null;
        this.selectedQuestionCount = null;
        this.lastQuizResult = null;
        this.lastActivityAt = null;
        this.sessionExpiredNotice = true;
    }

    public boolean consumeSessionExpiredNotice() {
        boolean currentNotice = sessionExpiredNotice;
        sessionExpiredNotice = false;
        return currentNotice;
    }

    public void clear() {
        this.currentUser = null;
        this.selectedCategory = null;
        this.selectedDifficulty = null;
        this.selectedQuestionCount = null;
        this.lastQuizResult = null;
        this.lastActivityAt = null;
        this.sessionExpiredNotice = false;
    }

    private void markActivityIfAuthenticated() {
        if (currentUser != null) {
            markActivity();
        }
    }

    private static long readSessionTimeoutMinutes() {
        String configuredValue = AppConfig.get("session.timeoutMinutes");
        if (configuredValue == null) {
            return DEFAULT_SESSION_TIMEOUT_MINUTES;
        }

        try {
            long parsed = Long.parseLong(configuredValue);
            return parsed > 0 ? parsed : DEFAULT_SESSION_TIMEOUT_MINUTES;
        } catch (NumberFormatException exception) {
            return DEFAULT_SESSION_TIMEOUT_MINUTES;
        }
    }
}
