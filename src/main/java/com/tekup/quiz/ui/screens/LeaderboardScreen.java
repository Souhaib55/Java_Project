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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LeaderboardScreen implements AppScreen {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM  HH:mm");
    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderboardScreen.class);
    private static final String[] MEDALS = {"🥇", "🥈", "🥉"};

    private final ScreenManager screenManager;
    private final SessionContext sessionContext;
    private final LeaderboardService leaderboardService;
    private final CategoryDao categoryDao;

    private final VBox root;
    private final Label subtitleLabel;
    private final Label feedbackLabel;
    private final ComboBox<FilterOption> filterComboBox;
    private final ListView<AttemptView> rowsListView;

    public LeaderboardScreen(ScreenManager screenManager,
                             SessionContext sessionContext,
                             LeaderboardService leaderboardService,
                             CategoryDao categoryDao) {
        this.screenManager = Objects.requireNonNull(screenManager);
        this.sessionContext = Objects.requireNonNull(sessionContext);
        this.leaderboardService = Objects.requireNonNull(leaderboardService);
        this.categoryDao = Objects.requireNonNull(categoryDao);

        // ── Header ────────────────────────────────────────────
        Label icon = new Label("🏆");
        icon.setStyle("-fx-font-size: 24px;");
        Label title = new Label("Leaderboard");
        title.getStyleClass().add("screen-title");
        HBox titleRow = new HBox(12, icon, title);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        subtitleLabel = new Label();
        subtitleLabel.getStyleClass().add("screen-subtitle");

        // ── Filter bar ────────────────────────────────────────
        Label filterLabel = new Label("Scope:");
        filterLabel.setStyle("-fx-text-fill: #7a8caa; -fx-font-size: 12px; -fx-font-weight: 600;");
        filterComboBox = new ComboBox<>();
        filterComboBox.setPrefWidth(260);
        filterComboBox.setOnAction(event -> loadRows());

        Button refreshButton = new Button("↻  Refresh");
        refreshButton.getStyleClass().add("nav-button");
        refreshButton.setOnAction(event -> { reloadFilters(); loadRows(); });

        HBox filterBar = new HBox(12, filterLabel, filterComboBox, refreshButton);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("stat-card");
        filterBar.setPadding(new Insets(12, 16, 12, 16));

        // ── Ranked list ───────────────────────────────────────
        Label listHeader = new Label("TOP RANKINGS");
        listHeader.setStyle("-fx-text-fill: #4a5a78; -fx-font-size: 11px; -fx-font-weight: 700;");

        rowsListView = new ListView<>();
        rowsListView.getStyleClass().add("modern-list");
        VBox.setVgrow(rowsListView, Priority.ALWAYS);

        rowsListView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(AttemptView item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }

                int rank = getIndex();

                // Rank badge
                String medalText = rank < MEDALS.length ? MEDALS[rank] : "#" + (rank + 1);
                Label rankLabel = new Label(medalText);
                rankLabel.setStyle(
                    "-fx-font-size: " + (rank < 3 ? "22px" : "14px") + "; " +
                    "-fx-text-fill: " + (rank < 3 ? "#f5c842" : "#4a5a78") + "; " +
                    "-fx-min-width: 38px; -fx-alignment: center; -fx-font-weight: 700;"
                );

                // Username
                Label userLbl = new Label(item.getUsername());
                userLbl.setStyle("-fx-text-fill: #e2e8f8; -fx-font-weight: 700; -fx-font-size: 14px;");

                // Category pill
                Label catPill = new Label(item.getCategoryName());
                catPill.getStyleClass().add("badge-info");

                // Date
                Label dateLbl = new Label(item.getAttemptedAt().format(DATE_FMT));
                dateLbl.setStyle("-fx-text-fill: #4a5a78; -fx-font-size: 11px;");

                VBox leftBlock = new VBox(3, userLbl, new HBox(6, catPill, dateLbl));
                ((HBox) leftBlock.getChildren().get(1)).setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(leftBlock, Priority.ALWAYS);

                // Score badge
                Label scoreLbl = new Label("⭐ " + item.getScore());
                scoreLbl.setStyle(
                    "-fx-background-color: " + (rank == 0 ? "rgba(245,200,66,0.20)" : "rgba(79,110,247,0.18)") + "; " +
                    "-fx-text-fill: " + (rank == 0 ? "#f5c842" : "#7b9bff") + "; " +
                    "-fx-background-radius: 20; " +
                    "-fx-padding: 5 14; " +
                    "-fx-font-weight: 800; -fx-font-size: 15px;"
                );

                HBox row = new HBox(14, rankLabel, leftBlock, scoreLbl);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(6, 8, 6, 8));
                setGraphic(row);
                setText(null);
            }
        });

        // ── Bottom nav ────────────────────────────────────────
        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.getStyleClass().add("feedback-error");

        Button historyButton = new Button("📋  My History");
        historyButton.getStyleClass().add("nav-button");
        historyButton.setOnAction(event -> screenManager.show(AppRoute.HISTORY));

        Button categoryButton = new Button("← Categories");
        categoryButton.getStyleClass().add("nav-button");
        categoryButton.setOnAction(event -> screenManager.show(AppRoute.CATEGORY));

        Button logoutButton = new Button("Logout");
        logoutButton.getStyleClass().add("danger-button");
        logoutButton.setOnAction(event -> { sessionContext.clear(); screenManager.show(AppRoute.AUTH); });

        HBox actions = new HBox(10, historyButton, categoryButton, logoutButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        root = new VBox(16, titleRow, subtitleLabel, filterBar, listHeader, rowsListView, actions, feedbackLabel);
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(28));
        VBox.setVgrow(rowsListView, Priority.ALWAYS);
    }

    @Override public String title() { return "QuizMaster — Leaderboard"; }
    @Override public Parent root() { return root; }

    @Override
    public void onShow() {
        User currentUser = sessionContext.getCurrentUser();
        if (currentUser == null) { screenManager.show(AppRoute.AUTH); return; }
        subtitleLabel.setText("Top performers across all categories  ·  " + currentUser.getUsername());
        setFeedback("", "feedback-error");
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
            Long selectedId = sessionContext.getSelectedCategory() == null ? null : sessionContext.getSelectedCategory().getId();
            FilterOption selected = options.stream()
                    .filter(o -> Objects.equals(o.categoryId(), selectedId))
                    .findFirst().orElse(options.get(0));
            filterComboBox.setValue(selected);
        } catch (RuntimeException exception) {
            LOGGER.error("Leaderboard filter reload failed", exception);
            filterComboBox.setItems(FXCollections.observableArrayList(List.of(FilterOption.global())));
            filterComboBox.setValue(FilterOption.global());
            setFeedback("Could not load category filters.", "feedback-error");
        }
    }

    private void loadRows() {
        try {
            FilterOption option = filterComboBox.getValue();
            LOGGER.info("Leaderboard load: scope={} categoryId={}", option == null || option.categoryId() == null ? "global" : "category", option == null ? null : option.categoryId());
            List<AttemptView> rows;
            if (option == null || option.categoryId() == null) {
                rows = leaderboardService.topGlobalRows();
            } else {
                rows = leaderboardService.topByCategoryRows(option.categoryId());
            }
            rowsListView.setItems(FXCollections.observableArrayList(rows));
            if (rows.isEmpty()) {
                setFeedback("No attempts recorded yet. Play a quiz to appear here!", "feedback-warning");
            } else {
                setFeedback("", "feedback-error");
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Leaderboard load failed", exception);
            rowsListView.setItems(FXCollections.emptyObservableList());
            setFeedback("Could not load leaderboard. Try refreshing.", "feedback-error");
        }
    }

    private record FilterOption(Long categoryId, String label) {
        private static FilterOption global() { return new FilterOption(null, "🌍  Global Top 10"); }
        @Override public String toString() { return label; }
    }

    private void setFeedback(String message, String styleClass) {
        feedbackLabel.getStyleClass().removeAll("feedback-error", "feedback-success", "feedback-warning");
        feedbackLabel.getStyleClass().add(styleClass);
        feedbackLabel.setText(message);
    }
}
