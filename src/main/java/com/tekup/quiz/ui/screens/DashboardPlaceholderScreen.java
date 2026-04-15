package com.tekup.quiz.ui.screens;

import com.tekup.quiz.model.Category;
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
import javafx.scene.layout.VBox;

import java.util.Objects;

public class DashboardPlaceholderScreen implements AppScreen {
    private final ScreenManager screenManager;
    private final SessionContext sessionContext;
    private final VBox root;
    private final Label contextLabel;

    public DashboardPlaceholderScreen(ScreenManager screenManager, SessionContext sessionContext) {
        this.screenManager = Objects.requireNonNull(screenManager, "screenManager must not be null");
        this.sessionContext = Objects.requireNonNull(sessionContext, "sessionContext must not be null");

        Label title = new Label("Flow Ready");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        contextLabel = new Label();
        contextLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2f4050;");

        Label info = new Label("Next sprint will replace this screen with the real quiz interface.");
        info.setStyle("-fx-text-fill: #4a5a6a;");

        Button backToCategoriesButton = new Button("Back to categories");
        backToCategoriesButton.setOnAction(event -> screenManager.show(AppRoute.CATEGORY));

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> {
            sessionContext.clear();
            screenManager.show(AppRoute.AUTH);
        });

        root = new VBox(14, title, contextLabel, info, backToCategoriesButton, logoutButton);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER_LEFT);
        root.setStyle("-fx-background-color: #eef6f8;");
    }

    @Override
    public String title() {
        return "Interactive Quiz - Dashboard Placeholder";
    }

    @Override
    public Parent root() {
        return root;
    }

    @Override
    public void onShow() {
        User user = sessionContext.getCurrentUser();
        Category category = sessionContext.getSelectedCategory();

        String username = user == null ? "Guest" : user.getUsername();
        String categoryName = category == null ? "None" : category.getName();
        contextLabel.setText("User: " + username + " | Selected category: " + categoryName);
    }
}
