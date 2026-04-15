package com.tekup.quiz.ui.screens;

import com.tekup.quiz.dao.CategoryDao;
import com.tekup.quiz.model.Category;
import com.tekup.quiz.model.Role;
import com.tekup.quiz.model.User;
import com.tekup.quiz.ui.AppRoute;
import com.tekup.quiz.ui.AppScreen;
import com.tekup.quiz.ui.ScreenManager;
import com.tekup.quiz.ui.SessionContext;
import com.tekup.quiz.util.FormValidator;
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
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;

public class AdminCategoryScreen implements AppScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminCategoryScreen.class);

    private final ScreenManager screenManager;
    private final SessionContext sessionContext;
    private final CategoryDao categoryDao;

    private final VBox root;
    private final Label subtitleLabel;
    private final Label feedbackLabel;
    private final ListView<Category> categoryListView;
    private final TextField categoryNameField;

    private Category selectedCategory;

    public AdminCategoryScreen(ScreenManager screenManager, SessionContext sessionContext, CategoryDao categoryDao) {
        this.screenManager = Objects.requireNonNull(screenManager, "screenManager must not be null");
        this.sessionContext = Objects.requireNonNull(sessionContext, "sessionContext must not be null");
        this.categoryDao = Objects.requireNonNull(categoryDao, "categoryDao must not be null");

        Label title = new Label("Category Management");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        subtitleLabel = new Label();
        subtitleLabel.setStyle("-fx-text-fill: #2f4050;");

        categoryListView = new ListView<>();
        categoryListView.setPrefHeight(320);
        categoryNameField = new TextField();
        categoryNameField.setPromptText("Category name");

        categoryListView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "#" + item.getId() + "  " + item.getName());
            }
        });
        categoryListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedCategory = newValue;
            if (newValue != null) {
                categoryNameField.setText(newValue.getName());
            }
        });

        Button saveButton = new Button("Save category");
        saveButton.setOnAction(event -> saveCategory());

        Button deleteButton = new Button("Delete selected");
        deleteButton.setOnAction(event -> deleteCategory());

        Button clearButton = new Button("Clear selection");
        clearButton.setOnAction(event -> clearForm());

        Button backButton = new Button("Back to dashboard");
        backButton.setOnAction(event -> screenManager.show(AppRoute.ADMIN_DASHBOARD));

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> {
            sessionContext.clear();
            screenManager.show(AppRoute.AUTH);
        });

        HBox formActions = new HBox(10, saveButton, deleteButton, clearButton, backButton, logoutButton);
        formActions.setAlignment(Pos.CENTER_LEFT);

        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setStyle("-fx-text-fill: #b00020;");

        root = new VBox(12, title, subtitleLabel, categoryListView, categoryNameField, formActions, feedbackLabel);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f3f8ef;");
    }

    @Override
    public String title() {
        return "Interactive Quiz - Admin Categories";
    }

    @Override
    public Parent root() {
        return root;
    }

    @Override
    public void onShow() {
        User currentUser = sessionContext.getCurrentUser();
        if (currentUser == null) {
            LOGGER.warn("Admin category access denied: no authenticated user");
            screenManager.show(AppRoute.AUTH);
            return;
        }
        if (currentUser.getRole() != Role.ADMIN) {
            LOGGER.warn("Admin category access denied: userId={} role={}", currentUser.getId(), currentUser.getRole());
            screenManager.show(AppRoute.CATEGORY);
            return;
        }

        subtitleLabel.setText("Manage category list (delete blocked when linked questions exist)");
        loadCategories();
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryDao.findAll();
            categoryListView.setItems(FXCollections.observableArrayList(categories));
            if (categories.isEmpty()) {
                feedbackLabel.setStyle("-fx-text-fill: #7a6200;");
                feedbackLabel.setText("No categories available.");
            } else {
                feedbackLabel.setText("");
            }
        } catch (RuntimeException exception) {
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Could not load categories.");
        }
    }

    private void saveCategory() {
        User actor = sessionContext.getCurrentUser();
        String actorName = actor == null ? "unknown" : actor.getUsername();
        long actorId = actor == null ? -1 : actor.getId();

        final String name;
        try {
            name = FormValidator.validateCategoryName(categoryNameField.getText());
        } catch (IllegalArgumentException exception) {
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText(exception.getMessage());
            return;
        }

        try {
            boolean updateMode = selectedCategory != null;
            Category toSave = selectedCategory == null
                    ? new Category(name)
                    : new Category(selectedCategory.getId(), name);
            Category saved = categoryDao.save(toSave);

            LOGGER.info(
                    "Admin category upsert successful: actorId={} actor={} mode={} categoryId={} categoryName={}",
                    actorId,
                    actorName,
                    updateMode ? "update" : "create",
                    saved.getId(),
                    saved.getName()
            );

            clearForm();
            loadCategories();
            feedbackLabel.setStyle("-fx-text-fill: #0a7d2f;");
            feedbackLabel.setText("Category saved: " + saved.getName());
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Admin category upsert failed: actorId={} actor={} categoryName={}",
                    actorId,
                    actorName,
                    name,
                    exception
            );
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText(friendlyCategoryError(exception));
        }
    }

    private void deleteCategory() {
        User actor = sessionContext.getCurrentUser();
        String actorName = actor == null ? "unknown" : actor.getUsername();
        long actorId = actor == null ? -1 : actor.getId();

        if (selectedCategory == null) {
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Select a category to delete.");
            return;
        }

        try {
            if (categoryDao.isInUse(selectedCategory.getId())) {
                feedbackLabel.setStyle("-fx-text-fill: #b00020;");
                feedbackLabel.setText("Cannot delete this category because it has linked questions.");
                return;
            }

            boolean deleted = categoryDao.deleteById(selectedCategory.getId());
            if (!deleted) {
                feedbackLabel.setStyle("-fx-text-fill: #b00020;");
                feedbackLabel.setText("Category was not deleted. It may no longer exist.");
                return;
            }

            LOGGER.info(
                    "Admin category delete successful: actorId={} actor={} categoryId={} categoryName={}",
                    actorId,
                    actorName,
                    selectedCategory.getId(),
                    selectedCategory.getName()
            );

            feedbackLabel.setStyle("-fx-text-fill: #0a7d2f;");
            feedbackLabel.setText("Category deleted.");
            clearForm();
            loadCategories();
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Admin category delete failed: actorId={} actor={} categoryId={} categoryName={}",
                    actorId,
                    actorName,
                    selectedCategory.getId(),
                    selectedCategory.getName(),
                    exception
            );
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText(friendlyDeleteError(exception));
        }
    }

    private void clearForm() {
        selectedCategory = null;
        categoryListView.getSelectionModel().clearSelection();
        categoryNameField.clear();
    }

    private String friendlyCategoryError(RuntimeException exception) {
        String message = exception.getMessage();
        if (message != null && (
                message.toLowerCase().contains("duplicate")
                        || message.toLowerCase().contains("already exists")
        )) {
            return "Category name already exists.";
        }
        return "Could not save category.";
    }

    private String friendlyDeleteError(RuntimeException exception) {
        String message = exception.getMessage();
        if (message != null && message.toLowerCase().contains("linked questions")) {
            return "Cannot delete this category because it has linked questions.";
        }
        return "Could not delete category.";
    }
}
