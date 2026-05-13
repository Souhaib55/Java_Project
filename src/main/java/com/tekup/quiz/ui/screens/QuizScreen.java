package com.tekup.quiz.ui.screens;

import com.tekup.quiz.model.Category;
import com.tekup.quiz.model.Difficulty;
import com.tekup.quiz.model.Question;
import com.tekup.quiz.model.QuestionType;
import com.tekup.quiz.model.QuizResult;
import com.tekup.quiz.model.User;
import com.tekup.quiz.service.QuizService;
import com.tekup.quiz.ui.AppRoute;
import com.tekup.quiz.ui.AppScreen;
import com.tekup.quiz.ui.ScreenManager;
import com.tekup.quiz.ui.SessionContext;
import com.tekup.quiz.util.AppConfig;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class QuizScreen implements AppScreen {
    private static final String[] MCQ_CODES = {"A", "B", "C", "D"};

    private final ScreenManager screenManager;
    private final SessionContext sessionContext;
    private final QuizService quizService;

    private final VBox root;
    private final Label setupLabel;
    private final Label progressLabel;
    private final ProgressBar progressBar;
    private final Label timerLabel;
    private final Label questionLabel;
    private final Label feedbackLabel;
    private final VBox optionsContainer;
    private final Button submitButton;
    private final Button skipButton;

    private List<Question> questions = List.of();
    private List<String> submittedAnswers = List.of();
    private ToggleGroup answerToggleGroup = new ToggleGroup();
    private Timeline timer;
    private int secondsPerQuestion;
    private int remainingSeconds;
    private int currentQuestionIndex;
    private boolean evaluating;

    public QuizScreen(ScreenManager screenManager, SessionContext sessionContext, QuizService quizService) {
        this.screenManager = Objects.requireNonNull(screenManager);
        this.sessionContext = Objects.requireNonNull(sessionContext);
        this.quizService = Objects.requireNonNull(quizService);

        // ── Header row ────────────────────────────────────────
        Label icon = new Label("📝");
        icon.setStyle("-fx-font-size: 24px;");
        Label title = new Label("Quiz");
        title.getStyleClass().add("screen-title");
        HBox titleRow = new HBox(12, icon, title);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        setupLabel = new Label();
        setupLabel.getStyleClass().add("screen-subtitle");

        // ── Progress row ──────────────────────────────────────
        progressLabel = new Label("Question 0/0");
        progressLabel.setStyle("-fx-text-fill: #7a8caa; -fx-font-size: 12px; -fx-font-weight: 600;");

        timerLabel = new Label("⏱  --");
        timerLabel.getStyleClass().add("timer-normal");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox progressRow = new HBox(12, progressLabel, spacer, timerLabel);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-pref-height: 7px;");

        // ── Question card ─────────────────────────────────────
        questionLabel = new Label();
        questionLabel.setWrapText(true);
        questionLabel.setStyle("-fx-text-fill: #e2e8f8; -fx-font-size: 16px; -fx-font-weight: 600;");

        VBox questionCard = new VBox(10, questionLabel);
        questionCard.getStyleClass().add("stat-card");
        questionCard.setMinHeight(80);

        // ── Options ───────────────────────────────────────────
        optionsContainer = new VBox(10);

        // ── Feedback ──────────────────────────────────────────
        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.getStyleClass().add("feedback-warning");

        // ── Buttons ───────────────────────────────────────────
        submitButton = new Button("Submit Answer  →");
        submitButton.getStyleClass().add("primary-button");
        submitButton.setStyle("-fx-font-size: 13px; -fx-padding: 11 22;");
        submitButton.setOnAction(event -> submitAndNext());

        skipButton = new Button("Skip");
        skipButton.getStyleClass().add("nav-button");
        skipButton.setOnAction(event -> skipAndNext());

        Button cancelButton = new Button("Cancel Quiz");
        cancelButton.getStyleClass().add("danger-button");
        cancelButton.setOnAction(event -> { stopTimer(); screenManager.show(AppRoute.CATEGORY); });

        HBox actions = new HBox(12, submitButton, skipButton, cancelButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        root = new VBox(16, titleRow, setupLabel, progressRow, progressBar, questionCard, optionsContainer, actions, feedbackLabel);
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(28));
    }

    @Override public String title() { return "QuizMaster — Quiz"; }
    @Override public Parent root() { return root; }

    @Override
    public void onShow() {
        setFeedback("", "feedback-warning");
        evaluating = false;
        User user = sessionContext.getCurrentUser();
        Category category = sessionContext.getSelectedCategory();
        Difficulty difficulty = sessionContext.getSelectedDifficulty();
        Integer questionCount = sessionContext.getSelectedQuestionCount();
        if (user == null) { screenManager.show(AppRoute.AUTH); return; }
        if (category == null || difficulty == null || questionCount == null || questionCount <= 0) { screenManager.show(AppRoute.CATEGORY); return; }
        secondsPerQuestion = readSecondsPerQuestion();
        setupLabel.setText("🧑 " + user.getUsername() + "  ·  📁 " + category.getName() + "  ·  " + difficulty + "  ·  " + questionCount + " questions");
        loadQuiz(category.getId(), difficulty, questionCount);
    }

    @Override
    public void onHide() { stopTimer(); }

    private void loadQuiz(long categoryId, Difficulty difficulty, int questionCount) {
        submitButton.setDisable(false);
        skipButton.setDisable(false);
        try {
            questions = quizService.generateQuiz(categoryId, difficulty, questionCount);
            submittedAnswers = new ArrayList<>(Collections.nCopies(questions.size(), ""));
            currentQuestionIndex = 0;
            renderCurrentQuestion();
            if (questions.size() < questionCount) {
                setFeedback("⚠  Only " + questions.size() + " question(s) available for this setup.", "feedback-warning");
            }
        } catch (IllegalStateException exception) {
            if (exception.getMessage() != null && exception.getMessage().toLowerCase(Locale.ROOT).contains("no questions are available")) {
                showEmptyState("No questions found for this selection.", "Try another category or difficulty, or ask an admin to add questions.");
                return;
            }
            showEmptyState("Could not start quiz.", "Failed to load questions. Check database connectivity.");
        } catch (RuntimeException exception) {
            showEmptyState("Could not start quiz.", "Failed to load questions. Check database connectivity.");
        }
    }

    private void showEmptyState(String heading, String detail) {
        optionsContainer.getChildren().clear();
        questionLabel.setText(heading);
        progressLabel.setText("Question 0/0");
        progressBar.setProgress(0);
        timerLabel.setText("⏱  --");
        setFeedback(detail, "feedback-error");
        submitButton.setDisable(true);
        skipButton.setDisable(true);
    }

    private void renderCurrentQuestion() {
        if (currentQuestionIndex >= questions.size()) { evaluateQuiz(); return; }
        Question current = questions.get(currentQuestionIndex);
        int total = questions.size();
        progressLabel.setText("Question " + (currentQuestionIndex + 1) + " / " + total);
        progressBar.setProgress((double) (currentQuestionIndex + 1) / total);
        questionLabel.setText(current.getPrompt());
        answerToggleGroup = new ToggleGroup();
        optionsContainer.getChildren().clear();
        List<String> options = current.getOptions();
        for (int i = 0; i < options.size(); i++) {
            String code = optionCode(current, i);
            String text = optionText(current, i, options.get(i));
            RadioButton rb = new RadioButton(text);
            rb.setUserData(code);
            rb.setToggleGroup(answerToggleGroup);
            rb.setWrapText(true);
            rb.getStyleClass().add("quiz-option");
            optionsContainer.getChildren().add(rb);
            if (code.equalsIgnoreCase(submittedAnswers.get(currentQuestionIndex))) {
                answerToggleGroup.selectToggle(rb);
            }
        }
        startTimer();
    }

    private String optionCode(Question q, int idx) {
        if (q.getType() == QuestionType.MCQ) return MCQ_CODES[idx];
        return q.getOptions().get(idx).trim().toUpperCase(Locale.ROOT);
    }

    private String optionText(Question q, int idx, String val) {
        if (q.getType() == QuestionType.MCQ) return MCQ_CODES[idx] + ".  " + val;
        return val;
    }

    private void submitAndNext() {
        if (evaluating || questions.isEmpty()) return;
        storeCurrentAnswer();
        moveToNext();
    }

    private void skipAndNext() {
        if (evaluating || questions.isEmpty()) return;
        submittedAnswers.set(currentQuestionIndex, "");
        moveToNext();
    }

    private void moveToNext() { stopTimer(); currentQuestionIndex++; renderCurrentQuestion(); }

    private void storeCurrentAnswer() {
        Toggle t = answerToggleGroup.getSelectedToggle();
        submittedAnswers.set(currentQuestionIndex, t == null || t.getUserData() == null ? "" : t.getUserData().toString());
    }

    private void startTimer() {
        stopTimer();
        remainingSeconds = Math.max(1, secondsPerQuestion);
        updateTimerLabel();
        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingSeconds--;
            updateTimerLabel();
            if (remainingSeconds <= 0 && !evaluating) {
                submittedAnswers.set(currentQuestionIndex, "");
                setFeedback("⏰  Time's up for question " + (currentQuestionIndex + 1) + "! Moving on…", "feedback-warning");
                moveToNext();
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.playFromStart();
    }

    private void updateTimerLabel() {
        int s = Math.max(0, remainingSeconds);
        timerLabel.setText("⏱  " + s + "s");
        timerLabel.getStyleClass().removeAll("timer-normal", "timer-warning", "timer-danger");
        if (s > secondsPerQuestion / 2) {
            timerLabel.getStyleClass().add("timer-normal");
        } else if (s > 5) {
            timerLabel.getStyleClass().add("timer-warning");
        } else {
            timerLabel.getStyleClass().add("timer-danger");
        }
    }

    private void stopTimer() { if (timer != null) { timer.stop(); timer = null; } }

    private void evaluateQuiz() {
        if (evaluating) return;
        evaluating = true;
        stopTimer();
        submitButton.setDisable(true);
        skipButton.setDisable(true);
        try {
            User user = sessionContext.getCurrentUser();
            Category category = sessionContext.getSelectedCategory();
            QuizResult result = quizService.evaluate(user.getId(), category.getId(), questions, submittedAnswers);
            sessionContext.setLastQuizResult(result);
            screenManager.show(AppRoute.RESULT);
        } catch (RuntimeException exception) {
            setFeedback("Could not finalize quiz. Please try again.", "feedback-error");
            submitButton.setDisable(false);
            skipButton.setDisable(false);
            evaluating = false;
        }
    }

    private int readSecondsPerQuestion() {
        try { return Integer.parseInt(AppConfig.get("quiz.secondsPerQuestion")); }
        catch (Exception e) { return 15; }
    }

    private void setFeedback(String message, String styleClass) {
        feedbackLabel.getStyleClass().removeAll("feedback-error", "feedback-success", "feedback-warning");
        feedbackLabel.getStyleClass().add(styleClass);
        feedbackLabel.setText(message);
    }
}
