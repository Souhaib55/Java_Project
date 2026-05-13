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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
    private final Label formModeLabel;

    private Category selectedCategory;

    public AdminCategoryScreen(ScreenManager screenManager, SessionContext sessionContext, CategoryDao categoryDao) {
        this.screenManager = Objects.requireNonNull(screenManager, "screenManager must not be null");
        this.sessionContext = Objects.requireNonNull(sessionContext, "sessionContext must not be null");
        this.categoryDao = Objects.requireNonNull(categoryDao, "categoryDao must not be null");

        // ── Header ────────────────────────────────────────────
        Label icon = new Label("📁");
        icon.setStyle("-fx-font-size: 24px;");
        Label title = new Label("Category Management");
        title.getStyleClass().add("screen-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backButton = new Button("← Dashboard");
        backButton.getStyleClass().add("nav-button");
        backButton.setOnAction(event -> screenManager.show(AppRoute.ADMIN_DASHBOARD));

        Button logoutButton = new Button("Logout");
        logoutButton.getStyleClass().add("danger-button");
        logoutButton.setOnAction(event -> { sessionContext.clear(); screenManager.show(AppRoute.AUTH); });

        HBox titleRow = new HBox(12, icon, title, spacer, backButton, logoutButton);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        subtitleLabel = new Label();
        subtitleLabel.getStyleClass().add("screen-subtitle");

        // ── Left: Category list ───────────────────────────────
        Label listHeader = new Label("EXISTING CATEGORIES");
        listHeader.setStyle("-fx-text-fill: #4a5a78; -fx-font-size: 11px; -fx-font-weight: 700;");

        categoryListView = new ListView<>();
        categoryListView.getStyleClass().add("modern-list");
        VBox.setVgrow(categoryListView, Priority.ALWAYS);
        categoryListView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label idBadge = new Label("#" + item.getId());
                idBadge.getStyleClass().add("badge-info");
                Label nameLbl = new Label(item.getName());
                nameLbl.setStyle("-fx-text-fill: #e2e8f8; -fx-font-weight: 600;");
                HBox row = new HBox(10, idBadge, nameLbl);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setText(null);
            }
        });
        // List selection listener will be added after form fields are initialized

        VBox leftPane = new VBox(10, listHeader, categoryListView);
        leftPane.setPrefWidth(320);
        VBox.setVgrow(leftPane, Priority.ALWAYS);

        // ── Right: Form panel ─────────────────────────────────
        Label formHeader = new Label("CREATE / EDIT CATEGORY");
        formHeader.setStyle("-fx-text-fill: #4a5a78; -fx-font-size: 11px; -fx-font-weight: 700;");

        formModeLabel = new Label("Fill in the name below to create a new category.");
        formModeLabel.setStyle("-fx-text-fill: #7a8caa; -fx-font-size: 12px;");
        formModeLabel.setWrapText(true);

        Label nameLabel = new Label("Category Name");
        nameLabel.setStyle("-fx-text-fill: #7a8caa; -fx-font-size: 12px; -fx-font-weight: 600;");
        categoryNameField = new TextField();
        categoryNameField.setPromptText("e.g. Science, History, Programming…");
        categoryNameField.setMaxWidth(Double.MAX_VALUE);

        categoryListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedCategory = newValue;
            if (newValue != null) {
                categoryNameField.setText(newValue.getName());
                formModeLabel.setText("✏  Editing: " + newValue.getName());
                formModeLabel.setStyle("-fx-text-fill: #f5a623; -fx-font-size: 12px; -fx-font-weight: 600;");
            }
        });

        Button saveButton = new Button("💾  Save Category");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setOnAction(event -> saveCategory());

        Button deleteButton = new Button("🗑  Delete Selected");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setOnAction(event -> deleteCategory());

        Button clearButton = new Button("✕  Clear");
        clearButton.getStyleClass().add("nav-button");
        clearButton.setMaxWidth(Double.MAX_VALUE);
        clearButton.setOnAction(event -> clearForm());

        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.getStyleClass().add("feedback-error");

        VBox formPane = new VBox(12,
                formHeader, formModeLabel,
                new VBox(5, nameLabel, categoryNameField),
                saveButton, deleteButton, clearButton,
                feedbackLabel
        );
        formPane.getStyleClass().add("stat-card");
        formPane.setPrefWidth(300);
        VBox.setVgrow(formPane, Priority.ALWAYS);

        HBox contentRow = new HBox(20, leftPane, formPane);
        HBox.setHgrow(leftPane, Priority.ALWAYS);
        contentRow.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(contentRow, Priority.ALWAYS);

        root = new VBox(18, titleRow, subtitleLabel, contentRow);
        root.getStyleClass().addAll("app-root", "screen-admin");
        root.setPadding(new Insets(28));
        VBox.setVgrow(contentRow, Priority.ALWAYS);
    }

    @Override public String title() { return "QuizMaster — Categories"; }
    @Override public Parent root() { return root; }

    @Override
    public void onShow() {
        User currentUser = sessionContext.getCurrentUser();
        if (currentUser == null) { LOGGER.warn("Admin category access denied: no user"); screenManager.show(AppRoute.AUTH); return; }
        if (currentUser.getRole() != Role.ADMIN) { screenManager.show(AppRoute.CATEGORY); return; }
        subtitleLabel.setText("Manage your quiz categories  ·  Delete is blocked when questions are linked");
        loadCategories();
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryDao.findAll();
            categoryListView.setItems(FXCollections.observableArrayList(categories));
            if (categories.isEmpty()) {
                setFeedback("No categories yet. Create your first one →", "feedback-warning");
            } else {
                setFeedback("", "feedback-error");
            }
        } catch (RuntimeException exception) {
            setFeedback("Could not load categories.", "feedback-error");
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
            setFeedback(exception.getMessage(), "feedback-error");
            return;
        }
        try {
            boolean updateMode = selectedCategory != null;
            Category toSave = selectedCategory == null
                    ? new Category(name)
                    : new Category(selectedCategory.getId(), name);
            Category saved = categoryDao.save(toSave);
            LOGGER.info("Admin category upsert: actorId={} mode={} categoryId={} name={}", actorId, updateMode ? "update" : "create", saved.getId(), saved.getName());
            clearForm();
            loadCategories();
            setFeedback("✅  Category saved: " + saved.getName(), "feedback-success");
        } catch (RuntimeException exception) {
            LOGGER.error("Admin category upsert failed: actorId={} name={}", actorId, name, exception);
            setFeedback(friendlyCategoryError(exception), "feedback-error");
        }
    }

    private void deleteCategory() {
        User actor = sessionContext.getCurrentUser();
        long actorId = actor == null ? -1 : actor.getId();
        if (selectedCategory == null) { setFeedback("Select a category first.", "feedback-error"); return; }
        try {
            if (categoryDao.isInUse(selectedCategory.getId())) {
                setFeedback("Cannot delete — this category has linked questions.", "feedback-error");
                return;
            }
            boolean deleted = categoryDao.deleteById(selectedCategory.getId());
            if (!deleted) { setFeedback("Category not found or already deleted.", "feedback-error"); return; }
            LOGGER.info("Admin category delete: actorId={} categoryId={} name={}", actorId, selectedCategory.getId(), selectedCategory.getName());
            clearForm();
            loadCategories();
            setFeedback("🗑  Category deleted.", "feedback-success");
        } catch (RuntimeException exception) {
            LOGGER.error("Admin category delete failed: actorId={} categoryId={}", actorId, selectedCategory.getId(), exception);
            setFeedback(friendlyDeleteError(exception), "feedback-error");
        }
    }

    private void clearForm() {
        selectedCategory = null;
        categoryListView.getSelectionModel().clearSelection();
        categoryNameField.clear();
        formModeLabel.setText("Fill in the name below to create a new category.");
        formModeLabel.setStyle("-fx-text-fill: #7a8caa; -fx-font-size: 12px;");
    }

    private String friendlyCategoryError(RuntimeException e) {
        String msg = e.getMessage();
        if (msg != null && (msg.toLowerCase().contains("duplicate") || msg.toLowerCase().contains("already exists"))) return "Category name already exists.";
        return "Could not save category.";
    }

    private String friendlyDeleteError(RuntimeException e) {
        String msg = e.getMessage();
        if (msg != null && msg.toLowerCase().contains("linked questions")) return "Cannot delete — linked questions exist.";
        return "Could not delete category.";
    }

    private void setFeedback(String message, String styleClass) {
        feedbackLabel.getStyleClass().removeAll("feedback-error", "feedback-success", "feedback-warning");
        feedbackLabel.getStyleClass().add(styleClass);
        feedbackLabel.setText(message);
    }
}
