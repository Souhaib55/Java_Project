package com.tekup.quiz.ui.screens;

import com.tekup.quiz.dao.CategoryDao;
import com.tekup.quiz.model.AttemptView;
import com.tekup.quiz.model.Category;
import com.tekup.quiz.model.User;
import com.tekup.quiz.service.LeaderboardService;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LeaderboardScreen implements AppScreen {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderboardScreen.class);

    private final ScreenManager screenManager;
    private final SessionContext sessionContext;
    private final LeaderboardService leaderboardService;
    private final CategoryDao categoryDao;

    private final VBox root;
    private final Label subtitleLabel;
    private final Label feedbackLabel;
    private final ComboBox<FilterOption> filterComboBox;
    private final ListView<String> rowsListView;

    public LeaderboardScreen(ScreenManager screenManager,
                             SessionContext sessionContext,
                             LeaderboardService leaderboardService,
                             CategoryDao categoryDao) {
        this.screenManager = Objects.requireNonNull(screenManager, "screenManager must not be null");
        this.sessionContext = Objects.requireNonNull(sessionContext, "sessionContext must not be null");
        this.leaderboardService = Objects.requireNonNull(leaderboardService, "leaderboardService must not be null");
        this.categoryDao = Objects.requireNonNull(categoryDao, "categoryDao must not be null");

        Label title = new Label("Leaderboard");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        subtitleLabel = new Label();
        subtitleLabel.setStyle("-fx-text-fill: #2f4050;");

        filterComboBox = new ComboBox<>();
        filterComboBox.setPrefWidth(300);
        filterComboBox.setOnAction(event -> loadRows());

        rowsListView = new ListView<>();
        rowsListView.setPrefHeight(380);

        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setStyle("-fx-text-fill: #b00020;");

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> {
            reloadFilters();
            loadRows();
        });

        Button historyButton = new Button("My history");
        historyButton.setOnAction(event -> screenManager.show(AppRoute.HISTORY));

        Button categoryButton = new Button("Back to category");
        categoryButton.setOnAction(event -> screenManager.show(AppRoute.CATEGORY));

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> {
            sessionContext.clear();
            screenManager.show(AppRoute.AUTH);
        });

        HBox filterBar = new HBox(10, new Label("View"), filterComboBox, refreshButton);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        HBox actions = new HBox(10, historyButton, categoryButton, logoutButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        root = new VBox(12, title, subtitleLabel, filterBar, rowsListView, actions, feedbackLabel);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f0f6f1;");
    }

    @Override
    public String title() {
        return "Interactive Quiz - Leaderboard";
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

        subtitleLabel.setText("Player: " + currentUser.getUsername() + " | Top rankings");
        feedbackLabel.setText("");
        reloadFilters();
        loadRows();
    }

    private void reloadFilters() {
        try {
            List<FilterOption> options = new ArrayList<>();
            options.add(FilterOption.global());
            for (Category category : categoryDao.findAll()) {
                options.add(new FilterOption(category.getId(), category.getName()));
            }

            filterComboBox.setItems(FXCollections.observableArrayList(options));

            Long selectedCategoryId = sessionContext.getSelectedCategory() == null
                    ? null
                    : sessionContext.getSelectedCategory().getId();

            FilterOption selected = options.stream()
                    .filter(option -> Objects.equals(option.categoryId(), selectedCategoryId))
                    .findFirst()
                    .orElse(options.get(0));
            filterComboBox.setValue(selected);
        } catch (RuntimeException exception) {
            LOGGER.error("Leaderboard filter reload failed", exception);
            filterComboBox.setItems(FXCollections.observableArrayList(List.of(FilterOption.global())));
            filterComboBox.setValue(FilterOption.global());
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Could not load categories for filtering. Try Refresh or return to category.");
        }
    }

    private void loadRows() {
        try {
            FilterOption option = filterComboBox.getValue();
            LOGGER.info(
                    "Leaderboard screen load requested: userId={} scope={} categoryId={}",
                    sessionContext.getCurrentUser() == null ? -1 : sessionContext.getCurrentUser().getId(),
                    option == null || option.categoryId() == null ? "global" : "category",
                    option == null ? null : option.categoryId()
            );

            List<AttemptView> rows;
            if (option == null || option.categoryId() == null) {
                rows = leaderboardService.topGlobalRows();
            } else {
                rows = leaderboardService.topByCategoryRows(option.categoryId());
            }

            if (rows.isEmpty()) {
                rowsListView.setItems(FXCollections.observableArrayList(List.of("No attempts yet.")));
                feedbackLabel.setText("");
                return;
            }

            List<String> formatted = new ArrayList<>();
            for (int index = 0; index < rows.size(); index++) {
                AttemptView row = rows.get(index);
                formatted.add(
                        "#" + (index + 1)
                                + "  " + row.getUsername()
                                + " | " + row.getCategoryName()
                                + " | Score " + row.getScore()
                                + " | Questions " + row.getTotalQuestions()
                                + " | " + row.getAttemptedAt().format(DATE_FORMATTER)
                );
            }
            rowsListView.setItems(FXCollections.observableArrayList(formatted));
            feedbackLabel.setText("");
        } catch (RuntimeException exception) {
            LOGGER.error("Leaderboard screen load failed", exception);
            rowsListView.setItems(FXCollections.observableArrayList(List.of()));
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Could not load leaderboard. Use Refresh or return to category.");
        }
    }

    private record FilterOption(Long categoryId, String label) {
        private static FilterOption global() {
            return new FilterOption(null, "Global Top 10");
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
