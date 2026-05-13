package com.tekup.quiz.ui.screens;

import com.tekup.quiz.model.AttemptView;
import com.tekup.quiz.model.User;
import com.tekup.quiz.service.HistoryService;
import com.tekup.quiz.ui.AppRoute;
import com.tekup.quiz.ui.AppScreen;
import com.tekup.quiz.ui.ScreenManager;
import com.tekup.quiz.ui.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class HistoryScreen implements AppScreen {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");
    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryScreen.class);

    private final ScreenManager screenManager;
    private final SessionContext sessionContext;
    private final HistoryService historyService;

    private final VBox root;
    private final Label subtitleLabel;
    private final Label feedbackLabel;
    private final ListView<AttemptView> rowsListView;

    public HistoryScreen(ScreenManager screenManager, SessionContext sessionContext, HistoryService historyService) {
        this.screenManager = Objects.requireNonNull(screenManager);
        this.sessionContext = Objects.requireNonNull(sessionContext);
        this.historyService = Objects.requireNonNull(historyService);

        // ── Header ────────────────────────────────────────────
        Label icon = new Label("📋");
        icon.setStyle("-fx-font-size: 24px;");
        Label title = new Label("My History");
        title.getStyleClass().add("screen-title");
        HBox titleRow = new HBox(12, icon, title);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        subtitleLabel = new Label();
        subtitleLabel.getStyleClass().add("screen-subtitle");

        // ── Attempt list ──────────────────────────────────────
        Label listHeader = new Label("RECENT ATTEMPTS");
        listHeader.setStyle("-fx-text-fill: #4a5a78; -fx-font-size: 11px; -fx-font-weight: 700;");

        rowsListView = new ListView<>();
        rowsListView.getStyleClass().add("modern-list");
        VBox.setVgrow(rowsListView, Priority.ALWAYS);

        rowsListView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(AttemptView item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }

                // Category badge (initial letter)
                String initial = item.getCategoryName().isEmpty() ? "?" : item.getCategoryName().substring(0, 1).toUpperCase();
                Label catBadge = new Label(initial);
                catBadge.setStyle(
                    "-fx-background-color: rgba(79,110,247,0.22); " +
                    "-fx-text-fill: #7b9bff; " +
                    "-fx-font-weight: 800; -fx-font-size: 14px; " +
                    "-fx-min-width: 38px; -fx-min-height: 38px; " +
                    "-fx-max-width: 38px; -fx-max-height: 38px; " +
                    "-fx-background-radius: 19; -fx-alignment: center;"
                );

                // Category name + date
                Label catName = new Label(item.getCategoryName());
                catName.setStyle("-fx-text-fill: #e2e8f8; -fx-font-weight: 700; -fx-font-size: 14px;");

                Label dateLbl = new Label(item.getAttemptedAt().format(DATE_FMT));
                dateLbl.setStyle("-fx-text-fill: #4a5a78; -fx-font-size: 11px;");

                VBox leftBlock = new VBox(2, catName, dateLbl);
                leftBlock.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(leftBlock, Priority.ALWAYS);

                // Score pill
                String scoreText = item.getScore() + " / " + item.getTotalQuestions() + " Q";
                Label scoreLbl = new Label("⭐ " + scoreText);
                scoreLbl.setStyle(
                    "-fx-background-color: rgba(79,110,247,0.18); " +
                    "-fx-text-fill: #7b9bff; " +
                    "-fx-background-radius: 20; " +
                    "-fx-padding: 4 12; " +
                    "-fx-font-weight: 700; -fx-font-size: 12px;"
                );

                HBox row = new HBox(12, catBadge, leftBlock, scoreLbl);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(6, 8, 6, 8));

                setGraphic(row);
                setText(null);
            }
        });

        // ── Actions ───────────────────────────────────────────
        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.getStyleClass().add("feedback-error");

        Button refreshButton = new Button("↻  Refresh");
        refreshButton.getStyleClass().add("nav-button");
        refreshButton.setOnAction(event -> loadRows());

        Button leaderboardButton = new Button("🏆  Leaderboard");
        leaderboardButton.getStyleClass().add("nav-button");
        leaderboardButton.setOnAction(event -> screenManager.show(AppRoute.LEADERBOARD));

        Button categoryButton = new Button("← Categories");
        categoryButton.getStyleClass().add("nav-button");
        categoryButton.setOnAction(event -> screenManager.show(AppRoute.CATEGORY));

        Button logoutButton = new Button("Logout");
        logoutButton.getStyleClass().add("danger-button");
        logoutButton.setOnAction(event -> { sessionContext.clear(); screenManager.show(AppRoute.AUTH); });

        HBox actions = new HBox(10, refreshButton, leaderboardButton, categoryButton, logoutButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        root = new VBox(16, titleRow, subtitleLabel, listHeader, rowsListView, actions, feedbackLabel);
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(28));
        VBox.setVgrow(rowsListView, Priority.ALWAYS);
    }

    @Override public String title() { return "QuizMaster — History"; }
    @Override public Parent root() { return root; }

    @Override
    public void onShow() {
        User currentUser = sessionContext.getCurrentUser();
        if (currentUser == null) { screenManager.show(AppRoute.AUTH); return; }
        subtitleLabel.setText("Showing your recent quiz attempts,  " + currentUser.getUsername());
        setFeedback("", "feedback-error");
        loadRows();
    }

    private void loadRows() {
        User currentUser = sessionContext.getCurrentUser();
        if (currentUser == null) { screenManager.show(AppRoute.AUTH); return; }
        try {
            LOGGER.info("History load: userId={}", currentUser.getId());
            List<AttemptView> rows = historyService.recentForUser(currentUser.getId());
            rowsListView.setItems(FXCollections.observableArrayList(rows));
            if (rows.isEmpty()) {
                setFeedback("No attempts yet — start your first quiz!", "feedback-warning");
            } else {
                setFeedback("", "feedback-error");
            }
        } catch (RuntimeException exception) {
            LOGGER.error("History load failed: userId={}", currentUser.getId(), exception);
            rowsListView.setItems(FXCollections.emptyObservableList());
            setFeedback("Could not load history. Try refreshing.", "feedback-error");
        }
    }

    private void setFeedback(String message, String styleClass) {
        feedbackLabel.getStyleClass().removeAll("feedback-error", "feedback-success", "feedback-warning");
        feedbackLabel.getStyleClass().add(styleClass);
        feedbackLabel.setText(message);
    }
}
