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
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HistoryScreen implements AppScreen {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryScreen.class);

    private final ScreenManager screenManager;
    private final SessionContext sessionContext;
    private final HistoryService historyService;

    private final VBox root;
    private final Label subtitleLabel;
    private final Label feedbackLabel;
    private final ListView<String> rowsListView;

    public HistoryScreen(ScreenManager screenManager, SessionContext sessionContext, HistoryService historyService) {
        this.screenManager = Objects.requireNonNull(screenManager, "screenManager must not be null");
        this.sessionContext = Objects.requireNonNull(sessionContext, "sessionContext must not be null");
        this.historyService = Objects.requireNonNull(historyService, "historyService must not be null");

        Label title = new Label("My History");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        subtitleLabel = new Label();
        subtitleLabel.setStyle("-fx-text-fill: #2f4050;");

        rowsListView = new ListView<>();
        rowsListView.setPrefHeight(400);

        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setStyle("-fx-text-fill: #b00020;");

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> loadRows());

        Button leaderboardButton = new Button("Back to leaderboard");
        leaderboardButton.setOnAction(event -> screenManager.show(AppRoute.LEADERBOARD));

        Button categoryButton = new Button("Back to category");
        categoryButton.setOnAction(event -> screenManager.show(AppRoute.CATEGORY));

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> {
            sessionContext.clear();
            screenManager.show(AppRoute.AUTH);
        });

        HBox actions = new HBox(10, refreshButton, leaderboardButton, categoryButton, logoutButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        root = new VBox(12, title, subtitleLabel, rowsListView, actions, feedbackLabel);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f3f1fa;");
    }

    @Override
    public String title() {
        return "Interactive Quiz - History";
    }

    @Override
    public Parent root() {
        return root;
    }

    @Override
    public void onShow() {
        User currentUser = sessionContext.getCurrentUser();
        if (currentUser == null) {
            screenManager.show(AppRoute.AUTH);
            return;
        }

        subtitleLabel.setText("Recent attempts for " + currentUser.getUsername());
        feedbackLabel.setText("");
        loadRows();
    }

    private void loadRows() {
        User currentUser = sessionContext.getCurrentUser();
        if (currentUser == null) {
            screenManager.show(AppRoute.AUTH);
            return;
        }

        try {
            LOGGER.info("History screen load requested: userId={} username={}", currentUser.getId(), currentUser.getUsername());
            List<AttemptView> rows = historyService.recentForUser(currentUser.getId());
            if (rows.isEmpty()) {
                rowsListView.setItems(FXCollections.observableArrayList(List.of("No attempts yet.")));
                feedbackLabel.setText("");
                return;
            }

            List<String> formatted = new ArrayList<>();
            for (AttemptView row : rows) {
                formatted.add(
                        row.getAttemptedAt().format(DATE_FORMATTER) +
                                " | " + row.getCategoryName() +
                                " | Score " + row.getScore() +
                                " | Questions " + row.getTotalQuestions()
                );
            }
            rowsListView.setItems(FXCollections.observableArrayList(formatted));
            feedbackLabel.setText("");
        } catch (RuntimeException exception) {
            LOGGER.error("History screen load failed: userId={} username={}", currentUser.getId(), currentUser.getUsername(), exception);
            rowsListView.setItems(FXCollections.observableArrayList(List.of()));
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Could not load history. Use Refresh or return to leaderboard.");
        }
    }
}
