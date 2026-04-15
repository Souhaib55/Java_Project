package com.tekup.quiz.ui.screens;

import com.tekup.quiz.dao.CategoryDao;
import com.tekup.quiz.model.Category;
import com.tekup.quiz.model.Difficulty;
import com.tekup.quiz.model.Role;
import com.tekup.quiz.model.User;
import com.tekup.quiz.ui.AppRoute;
import com.tekup.quiz.ui.AppScreen;
import com.tekup.quiz.ui.ScreenManager;
import com.tekup.quiz.ui.SessionContext;
import com.tekup.quiz.util.AppConfig;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;

public class CategorySelectionScreen implements AppScreen {
    private final ScreenManager screenManager;
    private final SessionContext sessionContext;
    private final CategoryDao categoryDao;

    private final VBox root;
    private final Label greetingLabel;
    private final Label feedbackLabel;
    private final ListView<Category> categoryListView;
    private final ComboBox<Difficulty> difficultyComboBox;
    private final Spinner<Integer> questionCountSpinner;
    private final Button adminButton;

    public CategorySelectionScreen(ScreenManager screenManager, SessionContext sessionContext, CategoryDao categoryDao) {
        this.screenManager = Objects.requireNonNull(screenManager, "screenManager must not be null");
        this.sessionContext = Objects.requireNonNull(sessionContext, "sessionContext must not be null");
        this.categoryDao = Objects.requireNonNull(categoryDao, "categoryDao must not be null");

        Label title = new Label("Choose a category");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        greetingLabel = new Label();
        greetingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2f4050;");

        categoryListView = new ListView<>();
        categoryListView.setPrefHeight(360);
        categoryListView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        difficultyComboBox = new ComboBox<>();
        difficultyComboBox.getItems().addAll(Difficulty.values());
        difficultyComboBox.setValue(Difficulty.MEDIUM);

        int defaultQuestionCount = getDefaultQuestionCount();
        questionCountSpinner = new Spinner<>();
        questionCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, defaultQuestionCount));
        questionCountSpinner.setEditable(true);

        GridPane configGrid = new GridPane();
        configGrid.setHgap(10);
        configGrid.setVgap(8);
        configGrid.add(new Label("Difficulty"), 0, 0);
        configGrid.add(difficultyComboBox, 1, 0);
        configGrid.add(new Label("Question count"), 0, 1);
        configGrid.add(questionCountSpinner, 1, 1);

        feedbackLabel = new Label();
        feedbackLabel.setStyle("-fx-text-fill: #b00020;");

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> loadCategories());

        Button continueButton = new Button("Start quiz");
        continueButton.setOnAction(event -> continueToQuiz());

        Button leaderboardButton = new Button("Leaderboard");
        leaderboardButton.setOnAction(event -> screenManager.show(AppRoute.LEADERBOARD));

        adminButton = new Button("Admin tools");
        adminButton.setOnAction(event -> screenManager.show(AppRoute.ADMIN_DASHBOARD));

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> logout());

        HBox actions = new HBox(10, refreshButton, continueButton, leaderboardButton, adminButton, logoutButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        root = new VBox(12, title, greetingLabel, configGrid, categoryListView, actions, feedbackLabel);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f6f8fb;");
    }

    @Override
    public String title() {
        return "Interactive Quiz - Category Selection";
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
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        adminButton.setVisible(isAdmin);
        adminButton.setManaged(isAdmin);

        String username = currentUser.getUsername();
        greetingLabel.setText("Welcome, " + username + ". Select your quiz category.");
        if (sessionContext.getSelectedDifficulty() != null) {
            difficultyComboBox.setValue(sessionContext.getSelectedDifficulty());
        }
        if (sessionContext.getSelectedQuestionCount() != null) {
            questionCountSpinner.getValueFactory().setValue(sessionContext.getSelectedQuestionCount());
        }
        feedbackLabel.setText("");
        loadCategories();
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryDao.findAll();
            categoryListView.setItems(FXCollections.observableArrayList(categories));
            if (categories.isEmpty()) {
                feedbackLabel.setStyle("-fx-text-fill: #7a6200;");
                feedbackLabel.setText("No categories found. Please add categories from admin tools.");
            } else {
                feedbackLabel.setText("");
            }
        } catch (RuntimeException exception) {
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Could not load categories. Check database connectivity.");
        }
    }

    private void continueToQuiz() {
        Category selected = categoryListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Please select a category first.");
            return;
        }

        Difficulty selectedDifficulty = difficultyComboBox.getValue();
        if (selectedDifficulty == null) {
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Please select a difficulty.");
            return;
        }

        int questionCount;
        try {
            questionCount = questionCountSpinner.getValue();
        } catch (RuntimeException exception) {
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Invalid question count value.");
            return;
        }
        if (questionCount <= 0) {
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Question count must be positive.");
            return;
        }

        sessionContext.setSelectedCategory(selected);
        sessionContext.setSelectedDifficulty(selectedDifficulty);
        sessionContext.setSelectedQuestionCount(questionCount);
        sessionContext.setLastQuizResult(null);
        feedbackLabel.setText("");
        screenManager.show(AppRoute.QUIZ);
    }

    private void logout() {
        sessionContext.clear();
        screenManager.show(AppRoute.AUTH);
    }

    private int getDefaultQuestionCount() {
        try {
            return Integer.parseInt(AppConfig.get("quiz.defaultQuestionCount"));
        } catch (Exception exception) {
            return 10;
        }
    }
}
