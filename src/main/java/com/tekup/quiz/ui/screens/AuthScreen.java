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
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

        // ── Brand header ──────────────────────────────────────
        Label brandIcon = new Label("🎯");
        brandIcon.setStyle("-fx-font-size: 40px;");

        Label brandName = new Label("QuizMaster");
        brandName.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #e2e8f8;");

        Label brandTagline = new Label("Test your knowledge, top the leaderboard");
        brandTagline.setStyle("-fx-font-size: 13px; -fx-text-fill: #7a8caa;");

        VBox brandBox = new VBox(4, brandIcon, brandName, brandTagline);
        brandBox.setAlignment(Pos.CENTER);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(99,120,220,0.18); -fx-pref-height: 1;");

        // ── Mode label ────────────────────────────────────────
        modeLabel = new Label("Sign in to your account");
        modeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #c7d2ee;");

        // ── Form fields ───────────────────────────────────────
        Label userLabel = new Label("Username");
        userLabel.setStyle("-fx-text-fill: #7a8caa; -fx-font-size: 12px; -fx-font-weight: 600;");
        usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setPrefWidth(320);

        Label passLabel = new Label("Password");
        passLabel.setStyle("-fx-text-fill: #7a8caa; -fx-font-size: 12px; -fx-font-weight: 600;");
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");

        Label roleLabel = new Label("Account role");
        roleLabel.setStyle("-fx-text-fill: #7a8caa; -fx-font-size: 12px; -fx-font-weight: 600;");
        roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll(Role.PLAYER, Role.ADMIN);
        roleComboBox.setValue(Role.PLAYER);
        roleComboBox.setDisable(true);
        roleComboBox.setPrefWidth(320);

        VBox roleRow = new VBox(4, roleLabel, roleComboBox);

        registerModeCheckbox = new CheckBox("Create a new account");
        registerModeCheckbox.setStyle("-fx-text-fill: #7b9bff; -fx-font-size: 13px;");
        registerModeCheckbox.setOnAction(event -> updateMode());

        submitButton = new Button("Sign In");
        submitButton.setDefaultButton(true);
        submitButton.getStyleClass().add("primary-button");
        submitButton.setMaxWidth(Double.MAX_VALUE);
        submitButton.setStyle("-fx-font-size: 14px; -fx-padding: 12 0;");
        submitButton.setOnAction(event -> handleSubmit());

        feedbackLabel = new Label();
        feedbackLabel.getStyleClass().add("feedback-error");
        feedbackLabel.setWrapText(true);
        feedbackLabel.setMaxWidth(320);

        VBox form = new VBox(10,
                new VBox(4, userLabel, usernameField),
                new VBox(4, passLabel, passwordField),
                roleRow,
                registerModeCheckbox,
                submitButton,
                feedbackLabel
        );

        // ── Card ──────────────────────────────────────────────
        VBox card = new VBox(20, brandBox, sep, modeLabel, form);
        card.getStyleClass().add("screen-card");
        card.setMaxWidth(400);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefWidth(400);

        // ── Page root ─────────────────────────────────────────
        root = new VBox(card);
        root.getStyleClass().addAll("app-root", "screen-auth");
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
    }

    @Override public String title() { return "QuizMaster — Sign In"; }
    @Override public Parent root() { return root; }

    @Override
    public void onShow() {
        registerModeCheckbox.setSelected(false);
        roleComboBox.setDisable(true);
        submitButton.setText("Sign In");
        modeLabel.setText("Sign in to your account");
        if (sessionContext.consumeSessionExpiredNotice()) {
            setFeedback("Session expired. Please sign in again.", "feedback-warning");
            usernameField.clear();
            passwordField.clear();
            return;
        }
        usernameField.clear();
        passwordField.clear();
        setFeedback("", "feedback-error");
    }

    private void setFeedback(String message, String styleClass) {
        feedbackLabel.getStyleClass().removeAll("feedback-error", "feedback-success", "feedback-warning");
        feedbackLabel.getStyleClass().add(styleClass);
        feedbackLabel.setText(message);
    }

    private void updateMode() {
        boolean register = registerModeCheckbox.isSelected();
        roleComboBox.setDisable(!register);
        modeLabel.setText(register ? "Create your account" : "Sign in to your account");
        submitButton.setText(register ? "Create Account" : "Sign In");
        setFeedback("", "feedback-error");
    }

    private void handleSubmit() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        try {
            User authenticatedUser;
            if (registerModeCheckbox.isSelected()) {
                Role selectedRole = roleComboBox.getValue() == null ? Role.PLAYER : roleComboBox.getValue();
                authenticatedUser = authService.register(username, password, selectedRole);
                setFeedback("Account created! Welcome, " + authenticatedUser.getUsername(), "feedback-success");
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
            setFeedback(exception.getMessage(), "feedback-error");
        } catch (RuntimeException exception) {
            setFeedback("Service unavailable. Check database settings and try again.", "feedback-error");
        }
    }
}
