package com.tekup.quiz.ui.screens;

import com.tekup.quiz.model.Role;
import com.tekup.quiz.model.User;
import com.tekup.quiz.ui.AppRoute;
import com.tekup.quiz.ui.AppScreen;
import com.tekup.quiz.ui.ScreenManager;
import com.tekup.quiz.ui.SessionContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Objects;

public class AdminDashboardScreen implements AppScreen {
    private final ScreenManager screenManager;
    private final SessionContext sessionContext;

    private final VBox root;
    private final Label subtitleLabel;
    private final Label feedbackLabel;

    public AdminDashboardScreen(ScreenManager screenManager, SessionContext sessionContext) {
        this.screenManager = Objects.requireNonNull(screenManager, "screenManager must not be null");
        this.sessionContext = Objects.requireNonNull(sessionContext, "sessionContext must not be null");

        // ── Page header ───────────────────────────────────────
        Label icon = new Label("🛠");
        icon.setStyle("-fx-font-size: 28px;");
        Label title = new Label("Admin Dashboard");
        title.getStyleClass().add("screen-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button playerFlowButton = new Button("← Back to Quiz");
        playerFlowButton.getStyleClass().add("nav-button");
        playerFlowButton.setOnAction(event -> screenManager.show(AppRoute.CATEGORY));

        Button logoutButton = new Button("Logout");
        logoutButton.getStyleClass().add("danger-button");
        logoutButton.setOnAction(event -> {
            sessionContext.clear();
            screenManager.show(AppRoute.AUTH);
        });

        HBox titleRow = new HBox(12, icon, title, spacer, playerFlowButton, logoutButton);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        subtitleLabel = new Label();
        subtitleLabel.getStyleClass().add("screen-subtitle");

        // ── Tool cards ────────────────────────────────────────
        VBox catCard = buildToolCard("📁", "Categories",
                "Create, rename or delete quiz categories.",
                "Manage Categories", () -> screenManager.show(AppRoute.ADMIN_CATEGORY));

        VBox qCard = buildToolCard("❓", "Questions",
                "Add MCQ and True/False questions to categories.",
                "Manage Questions", () -> screenManager.show(AppRoute.ADMIN_QUESTION));

        VBox csvCard = buildToolCard("📥", "CSV Import",
                "Bulk-import questions from a CSV file.",
                "Open CSV Tools", () -> screenManager.show(AppRoute.ADMIN_CSV));

        HBox cardRow = new HBox(16, catCard, qCard, csvCard);
        HBox.setHgrow(catCard, Priority.ALWAYS);
        HBox.setHgrow(qCard, Priority.ALWAYS);
        HBox.setHgrow(csvCard, Priority.ALWAYS);
        catCard.setMaxWidth(Double.MAX_VALUE);
        qCard.setMaxWidth(Double.MAX_VALUE);
        csvCard.setMaxWidth(Double.MAX_VALUE);

        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.getStyleClass().add("feedback-error");

        root = new VBox(20, titleRow, subtitleLabel, cardRow, feedbackLabel);
        root.getStyleClass().addAll("app-root", "screen-admin");
        root.setPadding(new Insets(32));
    }

    private VBox buildToolCard(String emoji, String cardTitle, String desc, String btnText, Runnable action) {
        Label iconLabel = new Label(emoji);
        iconLabel.getStyleClass().add("card-icon");

        Label titleLabel = new Label(cardTitle);
        titleLabel.getStyleClass().add("card-title");

        Label descLabel = new Label(desc);
        descLabel.getStyleClass().add("card-desc");
        descLabel.setWrapText(true);

        Button btn = new Button(btnText);
        btn.getStyleClass().add("primary-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(event -> action.run());

        VBox card = new VBox(10, iconLabel, titleLabel, descLabel, btn);
        card.getStyleClass().add("dashboard-card");
        card.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(descLabel, Priority.ALWAYS);
        return card;
    }

    @Override public String title() { return "QuizMaster — Admin Dashboard"; }
    @Override public Parent root() { return root; }

    @Override
    public void onShow() {
        User currentUser = sessionContext.getCurrentUser();
        if (currentUser == null) { screenManager.show(AppRoute.AUTH); return; }
        if (currentUser.getRole() != Role.ADMIN) { screenManager.show(AppRoute.CATEGORY); return; }
        subtitleLabel.setText("Welcome back, " + currentUser.getUsername() + "  ·  Manage your quiz content below");
        feedbackLabel.setText("");
    }
}
