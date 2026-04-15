package com.tekup.quiz.ui.screens;

import com.tekup.quiz.model.Category;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ResultScreen implements AppScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultScreen.class);

    private final ScreenManager screenManager;
    private final SessionContext sessionContext;
    private final ResultExportService resultExportService;

    private final VBox root;
    private final Label summaryLabel;
    private final Label detailLabel;
    private final Label feedbackLabel;

    public ResultScreen(ScreenManager screenManager, SessionContext sessionContext, ResultExportService resultExportService) {
        this.screenManager = Objects.requireNonNull(screenManager, "screenManager must not be null");
        this.sessionContext = Objects.requireNonNull(sessionContext, "sessionContext must not be null");
        this.resultExportService = Objects.requireNonNull(resultExportService, "resultExportService must not be null");

        Label title = new Label("Quiz Result");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        summaryLabel = new Label();
        summaryLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        detailLabel = new Label();
        detailLabel.setWrapText(true);
        detailLabel.setStyle("-fx-text-fill: #334455;");

        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setStyle("-fx-text-fill: #b00020;");

        Button exportButton = new Button("Export result");
        exportButton.setOnAction(event -> exportResult());

        Button leaderboardButton = new Button("View leaderboard");
        leaderboardButton.setOnAction(event -> this.screenManager.show(AppRoute.LEADERBOARD));

        Button historyButton = new Button("My history");
        historyButton.setOnAction(event -> this.screenManager.show(AppRoute.HISTORY));

        Button retryButton = new Button("Retry same setup");
        retryButton.setOnAction(event -> {
            sessionContext.setLastQuizResult(null);
            this.screenManager.show(AppRoute.QUIZ);
        });

        Button categoryButton = new Button("Choose another category");
        categoryButton.setOnAction(event -> {
            sessionContext.setLastQuizResult(null);
            this.screenManager.show(AppRoute.CATEGORY);
        });

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> {
            sessionContext.clear();
            this.screenManager.show(AppRoute.AUTH);
        });

        HBox actions = new HBox(10, exportButton, leaderboardButton, historyButton, retryButton, categoryButton, logoutButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        root = new VBox(14, title, summaryLabel, detailLabel, actions, feedbackLabel);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f9f7ef;");
    }

    @Override
    public String title() {
        return "Interactive Quiz - Result";
    }

    @Override
    public Parent root() {
        return root;
    }

    @Override
    public void onShow() {
        feedbackLabel.setText("");

        QuizResult result = sessionContext.getLastQuizResult();
        User user = sessionContext.getCurrentUser();
        Category category = sessionContext.getSelectedCategory();

        if (result == null || user == null || category == null) {
            summaryLabel.setText("No quiz result available.");
            detailLabel.setText("Start a quiz from the category screen first.");
            return;
        }

        int maxWeightedScore = result.getQuestions().stream().mapToInt(q -> q.getDifficulty().weight()).sum();
        double percentage = maxWeightedScore == 0 ? 0 : (result.getScore() * 100.0 / maxWeightedScore);

        summaryLabel.setText("Score: " + result.getScore() + " / " + maxWeightedScore);
        detailLabel.setText(
                "Player: " + user.getUsername() +
                        "\nCategory: " + category.getName() +
                        "\nQuestions answered: " + result.getTotalQuestions() +
                        "\nWeighted performance: " + String.format("%.1f", percentage) + "%"
        );
    }

    private void exportResult() {
        QuizResult result = sessionContext.getLastQuizResult();
        User user = sessionContext.getCurrentUser();
        if (result == null || user == null) {
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("No result to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export quiz result");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text file", "*.txt"));

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        fileChooser.setInitialFileName("quiz-result-" + user.getUsername() + "-" + timestamp + ".txt");

        File outputFile = fileChooser.showSaveDialog(root.getScene().getWindow());
        if (outputFile == null) {
            return;
        }

        LOGGER.info(
                "Result export initiated from UI: userId={} username={} path={}",
                user.getId(),
                user.getUsername(),
                outputFile.getAbsolutePath()
        );

        try {
            resultExportService.exportAsText(outputFile.toPath(), user.getUsername(), result);
            feedbackLabel.setStyle("-fx-text-fill: #0a7d2f;");
            feedbackLabel.setText("Result exported to: " + outputFile.getAbsolutePath());
        } catch (IOException exception) {
            LOGGER.error(
                    "Result export failed in UI (io): userId={} username={} path={}",
                    user.getId(),
                    user.getUsername(),
                    outputFile.getAbsolutePath(),
                    exception
            );
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Could not export result file. Check destination permissions and try again.");
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Result export failed in UI (runtime): userId={} username={} path={}",
                    user.getId(),
                    user.getUsername(),
                    outputFile.getAbsolutePath(),
                    exception
            );
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Export failed unexpectedly. Please try again.");
        }
    }
}
