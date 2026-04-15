package com.tekup.quiz.ui.screens;

import com.tekup.quiz.model.Role;
import com.tekup.quiz.model.User;
import com.tekup.quiz.service.AuthService;
import com.tekup.quiz.ui.AppRoute;
import com.tekup.quiz.ui.AppScreen;
import com.tekup.quiz.ui.ScreenManager;
import com.tekup.quiz.ui.SessionContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.Objects;

public class AuthScreen implements AppScreen {
    private final ScreenManager screenManager;
    private final SessionContext sessionContext;
    private final AuthService authService;

    private final VBox root;
    private final Label modeLabel;
    private final Label feedbackLabel;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final CheckBox registerModeCheckbox;
    private final ComboBox<Role> roleComboBox;
    private final Button submitButton;

    public AuthScreen(ScreenManager screenManager, SessionContext sessionContext, AuthService authService) {
        this.screenManager = Objects.requireNonNull(screenManager, "screenManager must not be null");
        this.sessionContext = Objects.requireNonNull(sessionContext, "sessionContext must not be null");
        this.authService = Objects.requireNonNull(authService, "authService must not be null");

        Label title = new Label("Interactive Quiz");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        modeLabel = new Label("Login");
        modeLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #333333;");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setAlignment(Pos.CENTER);

        usernameField = new TextField();
        usernameField.setPromptText("Username");

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        registerModeCheckbox = new CheckBox("Create new account");
        registerModeCheckbox.setOnAction(event -> updateMode());

        roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll(Role.PLAYER, Role.ADMIN);
        roleComboBox.setValue(Role.PLAYER);
        roleComboBox.setDisable(true);

        submitButton = new Button("Login");
        submitButton.setDefaultButton(true);
        submitButton.setOnAction(event -> handleSubmit());

        form.add(new Label("Username"), 0, 0);
        form.add(usernameField, 1, 0);
        form.add(new Label("Password"), 0, 1);
        form.add(passwordField, 1, 1);
        form.add(new Label("Role"), 0, 2);
        form.add(roleComboBox, 1, 2);

        feedbackLabel = new Label();
        feedbackLabel.setStyle("-fx-text-fill: #b00020;");

        root = new VBox(14, title, modeLabel, form, registerModeCheckbox, submitButton, feedbackLabel);
        root.setPadding(new Insets(28));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f8fbff, #edf5ff);");
    }

    @Override
    public String title() {
        return "Interactive Quiz - Authentication";
    }

    @Override
    public Parent root() {
        return root;
    }

    @Override
    public void onShow() {
        if (sessionContext.consumeSessionExpiredNotice()) {
            feedbackLabel.setStyle("-fx-text-fill: #7a6200;");
            feedbackLabel.setText("Session expired due to inactivity. Please log in again.");
            return;
        }
        feedbackLabel.setStyle("-fx-text-fill: #b00020;");
        feedbackLabel.setText("");
    }

    private void updateMode() {
        boolean registerMode = registerModeCheckbox.isSelected();
        roleComboBox.setDisable(!registerMode);
        modeLabel.setText(registerMode ? "Register" : "Login");
        submitButton.setText(registerMode ? "Register" : "Login");
        feedbackLabel.setText("");
    }

    private void handleSubmit() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        try {
            User authenticatedUser;
            if (registerModeCheckbox.isSelected()) {
                Role selectedRole = roleComboBox.getValue() == null ? Role.PLAYER : roleComboBox.getValue();
                authenticatedUser = authService.register(username, password, selectedRole);
                feedbackLabel.setStyle("-fx-text-fill: #0a7d2f;");
                feedbackLabel.setText("Registration successful. You are now logged in.");
            } else {
                authenticatedUser = authService.login(username, password);
            }

            sessionContext.setCurrentUser(authenticatedUser);
            sessionContext.setSelectedCategory(null);
            sessionContext.setSelectedDifficulty(null);
            sessionContext.setSelectedQuestionCount(null);
            sessionContext.setLastQuizResult(null);
            passwordField.clear();
            screenManager.show(AppRoute.CATEGORY);
        } catch (IllegalArgumentException exception) {
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText(exception.getMessage());
        } catch (RuntimeException exception) {
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Service unavailable. Check database settings and try again.");
        }
    }
}
