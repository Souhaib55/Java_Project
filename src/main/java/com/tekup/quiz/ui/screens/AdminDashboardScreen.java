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

        Label title = new Label("Admin Tools");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        subtitleLabel = new Label();
        subtitleLabel.setStyle("-fx-text-fill: #2f4050;");

        Button categoriesButton = new Button("Manage categories");
        categoriesButton.setOnAction(event -> screenManager.show(AppRoute.ADMIN_CATEGORY));

        Button questionsButton = new Button("Manage questions");
        questionsButton.setOnAction(event -> screenManager.show(AppRoute.ADMIN_QUESTION));

        Button csvButton = new Button("CSV tools");
        csvButton.setOnAction(event -> screenManager.show(AppRoute.ADMIN_CSV));

        Button playerFlowButton = new Button("Back to category selection");
        playerFlowButton.setOnAction(event -> screenManager.show(AppRoute.CATEGORY));

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> {
            sessionContext.clear();
            screenManager.show(AppRoute.AUTH);
        });

        HBox actions = new HBox(10, categoriesButton, questionsButton, csvButton, playerFlowButton, logoutButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setStyle("-fx-text-fill: #b00020;");

        root = new VBox(14, title, subtitleLabel, actions, feedbackLabel);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f8f3ea;");
    }

    @Override
    public String title() {
        return "Interactive Quiz - Admin Dashboard";
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
        if (currentUser.getRole() != Role.ADMIN) {
            screenManager.show(AppRoute.CATEGORY);
            return;
        }

        subtitleLabel.setText("Admin user: " + currentUser.getUsername());
        feedbackLabel.setText("");
    }
}
