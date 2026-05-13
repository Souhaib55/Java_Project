package com.tekup.quiz.ui.screens;

import com.tekup.quiz.model.Category;
import com.tekup.quiz.model.McqQuestion;
import com.tekup.quiz.model.Question;
import com.tekup.quiz.model.QuizResult;
import com.tekup.quiz.model.User;
import com.tekup.quiz.service.ResultExportService;
import com.tekup.quiz.ui.AppRoute;
import com.tekup.quiz.ui.AppScreen;
import com.tekup.quiz.ui.ScreenManager;
import com.tekup.quiz.ui.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class ResultScreen implements AppScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultScreen.class);

    private final ScreenManager screenManager;
    private final SessionContext sessionContext;
    private final ResultExportService resultExportService;

    private final VBox root;
    private final Label scoreLabel;
    private final Label scoreMaxLabel;
    private final Label percentageLabel;
    private final ProgressBar percentBar;
    private final Label detailLabel;
    private final Label feedbackLabel;
    private final VBox breakdownContainer;

    public ResultScreen(ScreenManager screenManager, SessionContext sessionContext, ResultExportService resultExportService) {
        this.screenManager = Objects.requireNonNull(screenManager);
        this.sessionContext = Objects.requireNonNull(sessionContext);
        this.resultExportService = Objects.requireNonNull(resultExportService);

        // ── Header ────────────────────────────────────────────
        Label icon = new Label("🏁");
        icon.setStyle("-fx-font-size: 24px;");
        Label title = new Label("Quiz Result");
        title.getStyleClass().add("screen-title");
        HBox titleRow = new HBox(12, icon, title);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        // ── Score hero card ───────────────────────────────────
        Label trophyLabel = new Label("🏆");
        trophyLabel.setStyle("-fx-font-size: 42px;");

        scoreLabel = new Label("—");
        scoreLabel.getStyleClass().add("hero-score");

        scoreMaxLabel = new Label("out of —");
        scoreMaxLabel.getStyleClass().add("hero-score-label");

        percentageLabel = new Label();
        percentageLabel.setStyle("-fx-text-fill: #4f6ef7; -fx-font-size: 18px; -fx-font-weight: 700;");

        percentBar = new ProgressBar(0);
        percentBar.setMaxWidth(300);
        percentBar.setPrefHeight(10);

        detailLabel = new Label();
        detailLabel.setStyle("-fx-text-fill: #7a8caa; -fx-font-size: 13px;");
        detailLabel.setWrapText(true);

        VBox scoreBlock = new VBox(6, trophyLabel, scoreLabel, scoreMaxLabel, percentageLabel, percentBar, detailLabel);
        scoreBlock.setAlignment(Pos.CENTER);

        VBox heroCard = new VBox(16, scoreBlock);
        heroCard.getStyleClass().add("screen-card");
        heroCard.setAlignment(Pos.CENTER);
        heroCard.setPrefWidth(380);
        heroCard.setMaxWidth(420);

        HBox heroRow = new HBox(heroCard);
        heroRow.setAlignment(Pos.CENTER_LEFT);

        // ── Question breakdown ────────────────────────────────
        Label breakdownHeader = new Label("QUESTION BREAKDOWN");
        breakdownHeader.setStyle("-fx-text-fill: #4a5a78; -fx-font-size: 11px; -fx-font-weight: 700;");

        breakdownContainer = new VBox(8);

        ScrollPane breakdownScroll = new ScrollPane(breakdownContainer);
        breakdownScroll.setFitToWidth(true);
        breakdownScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        breakdownScroll.setPrefHeight(260);
        VBox.setVgrow(breakdownScroll, Priority.ALWAYS);

        VBox breakdownSection = new VBox(10, breakdownHeader, breakdownScroll);
        breakdownSection.getStyleClass().add("stat-card");
        VBox.setVgrow(breakdownSection, Priority.ALWAYS);

        // ── Actions ───────────────────────────────────────────
        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.getStyleClass().add("feedback-error");

        Button exportButton = new Button("📄  Export Result");
        exportButton.getStyleClass().add("primary-button");
        exportButton.setOnAction(event -> exportResult());

        Button leaderboardButton = new Button("🏆  Leaderboard");
        leaderboardButton.getStyleClass().add("nav-button");
        leaderboardButton.setOnAction(event -> screenManager.show(AppRoute.LEADERBOARD));

        Button historyButton = new Button("📋  My History");
        historyButton.getStyleClass().add("nav-button");
        historyButton.setOnAction(event -> screenManager.show(AppRoute.HISTORY));

        Button retryButton = new Button("↻  Retry");
        retryButton.getStyleClass().add("nav-button");
        retryButton.setOnAction(event -> { sessionContext.setLastQuizResult(null); screenManager.show(AppRoute.QUIZ); });

        Button categoryButton = new Button("← Categories");
        categoryButton.getStyleClass().add("nav-button");
        categoryButton.setOnAction(event -> { sessionContext.setLastQuizResult(null); screenManager.show(AppRoute.CATEGORY); });

        Button logoutButton = new Button("Logout");
        logoutButton.getStyleClass().add("danger-button");
        logoutButton.setOnAction(event -> { sessionContext.clear(); screenManager.show(AppRoute.AUTH); });

        HBox actions = new HBox(10, exportButton, leaderboardButton, historyButton, retryButton, categoryButton, logoutButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        root = new VBox(18, titleRow, heroRow, breakdownSection, actions, feedbackLabel);
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(28));
        VBox.setVgrow(breakdownSection, Priority.ALWAYS);
    }

    @Override public String title() { return "QuizMaster — Result"; }
    @Override public Parent root() { return root; }

    @Override
    public void onShow() {
        setFeedback("", "feedback-error");
        QuizResult result = sessionContext.getLastQuizResult();
        User user = sessionContext.getCurrentUser();
        Category category = sessionContext.getSelectedCategory();

        if (result == null || user == null || category == null) {
            scoreLabel.setText("—");
            scoreMaxLabel.setText("No result");
            percentageLabel.setText("");
            percentBar.setProgress(0);
            detailLabel.setText("Start a quiz from the category screen first.");
            breakdownContainer.getChildren().clear();
            return;
        }

        int maxWeighted = result.getQuestions().stream().mapToInt(q -> q.getDifficulty().weight()).sum();
        double pct = maxWeighted == 0 ? 0 : (result.getScore() * 100.0 / maxWeighted);

        scoreLabel.setText(String.valueOf(result.getScore()));
        scoreMaxLabel.setText("out of " + maxWeighted + " weighted pts");
        percentageLabel.setText(String.format("%.1f%%", pct));
        percentBar.setProgress(pct / 100.0);
        detailLabel.setText(
                "Player: " + user.getUsername() +
                "  ·  Category: " + category.getName() +
                "  ·  " + result.getTotalQuestions() + " questions answered"
        );

        buildBreakdown(result);
    }

    private void buildBreakdown(QuizResult result) {
        breakdownContainer.getChildren().clear();
        List<Question> questions = result.getQuestions();
        // We don't have access to submitted answers here, so show correct answer vs status
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            String diffStyle = switch (q.getDifficulty()) {
                case EASY -> "badge-easy";
                case MEDIUM -> "badge-medium";
                case HARD -> "badge-hard";
            };
            Label diffBadge = new Label(q.getDifficulty().name());
            diffBadge.getStyleClass().add(diffStyle);

            Label numLabel = new Label("Q" + (i + 1));
            numLabel.setStyle("-fx-text-fill: #4a5a78; -fx-font-size: 11px; -fx-font-weight: 700; -fx-min-width: 28px;");

            Label promptLabel = new Label(q.getPrompt().length() > 70 ? q.getPrompt().substring(0, 67) + "…" : q.getPrompt());
            promptLabel.setStyle("-fx-text-fill: #c7d2ee; -fx-font-size: 13px;");
            promptLabel.setWrapText(true);
            HBox.setHgrow(promptLabel, Priority.ALWAYS);

            Label correctLabel = new Label("✓ " + q.getCorrectAnswer());
            correctLabel.setStyle("-fx-text-fill: #3ecf8e; -fx-font-size: 11px; -fx-font-weight: 600;");

            HBox topRow = new HBox(8, numLabel, diffBadge, promptLabel, correctLabel);
            topRow.setAlignment(Pos.CENTER_LEFT);

            VBox card = new VBox(4, topRow);
            card.setStyle(
                "-fx-background-color: #1a2640; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: rgba(99,120,220,0.15); " +
                "-fx-border-radius: 10; " +
                "-fx-padding: 10 14;"
            );
            breakdownContainer.getChildren().add(card);
        }
    }

    private void exportResult() {
        QuizResult result = sessionContext.getLastQuizResult();
        User user = sessionContext.getCurrentUser();
        if (result == null || user == null) { setFeedback("No result to export.", "feedback-error"); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Quiz Result");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text file", "*.txt"));
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        fc.setInitialFileName("quiz-result-" + user.getUsername() + "-" + ts + ".txt");
        File outputFile = fc.showSaveDialog(root.getScene().getWindow());
        if (outputFile == null) return;
        LOGGER.info("Result export: userId={} path={}", user.getId(), outputFile.getAbsolutePath());
        try {
            resultExportService.exportAsText(outputFile.toPath(), user.getUsername(), result);
            setFeedback("✅  Result exported: " + outputFile.getAbsolutePath(), "feedback-success");
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Result export failed: userId={} path={}", user.getId(), outputFile.getAbsolutePath(), exception);
            setFeedback("Export failed. Check destination permissions.", "feedback-error");
        }
    }

    private void setFeedback(String message, String styleClass) {
        feedbackLabel.getStyleClass().removeAll("feedback-error", "feedback-success", "feedback-warning");
        feedbackLabel.getStyleClass().add(styleClass);
        feedbackLabel.setText(message);
    }
}
