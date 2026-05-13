package com.tekup.quiz.ui.screens;

import com.tekup.quiz.dao.CategoryDao;
import com.tekup.quiz.dao.QuestionDao;
import com.tekup.quiz.model.Category;
import com.tekup.quiz.model.Difficulty;
import com.tekup.quiz.model.Question;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;

public class CategorySelectionScreen implements AppScreen {
    private static final int MAX_REQUESTABLE_QUESTIONS = 50;

    private final ScreenManager screenManager;
    private final SessionContext sessionContext;
    private final CategoryDao categoryDao;
    private final QuestionDao questionDao;

    private final VBox root;
    private final Label greetingLabel;
    private final Label availabilityLabel;
    private final Label feedbackLabel;
    private final ListView<Category> categoryListView;
    private final ComboBox<Difficulty> difficultyComboBox;
    private final Spinner<Integer> questionCountSpinner;
    private final Button continueButton;
    private final Button adminButton;

    public CategorySelectionScreen(ScreenManager screenManager,
                                   SessionContext sessionContext,
                                   CategoryDao categoryDao,
                                   QuestionDao questionDao) {
        this.screenManager = Objects.requireNonNull(screenManager);
        this.sessionContext = Objects.requireNonNull(sessionContext);
        this.categoryDao = Objects.requireNonNull(categoryDao);
        this.questionDao = Objects.requireNonNull(questionDao);

        // ── Header ────────────────────────────────────────────
        Label appIcon = new Label("🎯");
        appIcon.setStyle("-fx-font-size: 26px;");
        Label title = new Label("Choose a Category");
        title.getStyleClass().add("screen-title");
        HBox titleRow = new HBox(12, appIcon, title);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        greetingLabel = new Label();
        greetingLabel.getStyleClass().add("screen-subtitle");

        // ── Category list ─────────────────────────────────────
        Label catHeader = new Label("QUIZ CATEGORIES");
        catHeader.setStyle("-fx-text-fill: #4a5a78; -fx-font-size: 11px; -fx-font-weight: 700;");

        categoryListView = new ListView<>();
        categoryListView.getStyleClass().add("modern-list");
        VBox.setVgrow(categoryListView, Priority.ALWAYS);
        categoryListView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                // Initial letter avatar
                String initial = item.getName().isEmpty() ? "?" : item.getName().substring(0, 1).toUpperCase();
                Label avatar = new Label(initial);
                avatar.setStyle(
                    "-fx-background-color: rgba(79,110,247,0.22); " +
                    "-fx-text-fill: #7b9bff; " +
                    "-fx-font-weight: 800; " +
                    "-fx-font-size: 14px; " +
                    "-fx-min-width: 34px; -fx-min-height: 34px; " +
                    "-fx-max-width: 34px; -fx-max-height: 34px; " +
                    "-fx-background-radius: 17; " +
                    "-fx-alignment: center;"
                );
                Label nameLbl = new Label(item.getName());
                nameLbl.setStyle("-fx-text-fill: #e2e8f8; -fx-font-weight: 600; -fx-font-size: 14px;");
                HBox row = new HBox(12, avatar, nameLbl);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(4, 0, 4, 0));
                setGraphic(row);
                setText(null);
            }
        });

        // ── Config card ───────────────────────────────────────
        Label configHeader = new Label("QUIZ SETUP");
        configHeader.setStyle("-fx-text-fill: #4a5a78; -fx-font-size: 11px; -fx-font-weight: 700;");

        Label diffLabel = new Label("Difficulty");
        diffLabel.setStyle("-fx-text-fill: #7a8caa; -fx-font-size: 12px; -fx-font-weight: 600;");
        difficultyComboBox = new ComboBox<>();
        difficultyComboBox.getItems().addAll(Difficulty.values());
        difficultyComboBox.setValue(Difficulty.MEDIUM);
        difficultyComboBox.setMaxWidth(Double.MAX_VALUE);
        difficultyComboBox.setOnAction(event -> updateQuestionAvailability());

        Label countLabel = new Label("Number of Questions");
        countLabel.setStyle("-fx-text-fill: #7a8caa; -fx-font-size: 12px; -fx-font-weight: 600;");
        int defaultCount = getDefaultQuestionCount();
        questionCountSpinner = new Spinner<>();
        questionCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, MAX_REQUESTABLE_QUESTIONS, defaultCount));
        questionCountSpinner.setEditable(true);
        questionCountSpinner.setMaxWidth(Double.MAX_VALUE);
        questionCountSpinner.focusedProperty().addListener((obs, old, focused) -> { if (!focused) commitSpinnerValue(); });

        availabilityLabel = new Label("Select a category to see available questions.");
        availabilityLabel.setStyle("-fx-text-fill: #7a8caa; -fx-font-size: 12px;");
        availabilityLabel.setWrapText(true);

        VBox configCard = new VBox(10,
                configHeader,
                new VBox(5, diffLabel, difficultyComboBox),
                new VBox(5, countLabel, questionCountSpinner),
                availabilityLabel
        );
        configCard.getStyleClass().add("stat-card");
        configCard.setPrefWidth(280);

        // ── Left pane (category list) ─────────────────────────
        VBox leftPane = new VBox(10, catHeader, categoryListView);
        VBox.setVgrow(leftPane, Priority.ALWAYS);
        HBox.setHgrow(leftPane, Priority.ALWAYS);

        HBox contentRow = new HBox(20, leftPane, configCard);
        contentRow.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(contentRow, Priority.ALWAYS);

        // ── Action bar ────────────────────────────────────────
        feedbackLabel = new Label();
        feedbackLabel.getStyleClass().add("feedback-error");
        feedbackLabel.setWrapText(true);

        continueButton = new Button("▶  Start Quiz");
        continueButton.getStyleClass().add("primary-button");
        continueButton.setStyle("-fx-font-size: 14px; -fx-padding: 11 24;");
        continueButton.setOnAction(event -> continueToQuiz());

        Button refreshButton = new Button("↻  Refresh");
        refreshButton.getStyleClass().add("nav-button");
        refreshButton.setOnAction(event -> loadCategories());

        Button leaderboardButton = new Button("🏆  Leaderboard");
        leaderboardButton.getStyleClass().add("nav-button");
        leaderboardButton.setOnAction(event -> screenManager.show(AppRoute.LEADERBOARD));

        adminButton = new Button("🛠  Admin Tools");
        adminButton.getStyleClass().add("nav-button");
        adminButton.setOnAction(event -> screenManager.show(AppRoute.ADMIN_DASHBOARD));

        Button logoutButton = new Button("Logout");
        logoutButton.getStyleClass().add("danger-button");
        logoutButton.setOnAction(event -> logout());

        HBox actions = new HBox(10, refreshButton, continueButton, leaderboardButton, adminButton, logoutButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        root = new VBox(16, titleRow, greetingLabel, contentRow, actions, feedbackLabel);
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(28));
        VBox.setVgrow(contentRow, Priority.ALWAYS);

        categoryListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> updateQuestionAvailability());
    }

    @Override public String title() { return "QuizMaster — Select Category"; }
    @Override public Parent root() { return root; }

    @Override
    public void onShow() {
        User currentUser = sessionContext.getCurrentUser();
        if (currentUser == null) { screenManager.show(AppRoute.AUTH); return; }
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        adminButton.setVisible(isAdmin);
        adminButton.setManaged(isAdmin);
        greetingLabel.setText("Welcome back, " + currentUser.getUsername() + "  ·  Pick a category and start your quiz");
        if (sessionContext.getSelectedDifficulty() != null) difficultyComboBox.setValue(sessionContext.getSelectedDifficulty());
        if (sessionContext.getSelectedQuestionCount() != null) questionCountSpinner.getValueFactory().setValue(Math.max(1, sessionContext.getSelectedQuestionCount()));
        setFeedback("", "feedback-error");
        loadCategories();
        updateQuestionAvailability();
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryDao.findAll();
            categoryListView.setItems(FXCollections.observableArrayList(categories));
            if (categories.isEmpty()) {
                continueButton.setDisable(true);
                availabilityLabel.setText("No categories available yet.");
                setFeedback("No categories found. An admin needs to add categories first.", "feedback-warning");
            } else {
                restoreCategorySelection(categories);
                setFeedback("", "feedback-error");
            }
            updateQuestionAvailability();
        } catch (RuntimeException exception) {
            continueButton.setDisable(true);
            setFeedback("Could not load categories. Check database connectivity.", "feedback-error");
        }
    }

    private void continueToQuiz() {
        commitSpinnerValue();
        Category selected = categoryListView.getSelectionModel().getSelectedItem();
        if (selected == null) { setFeedback("Please select a category first.", "feedback-error"); return; }
        Difficulty selectedDifficulty = difficultyComboBox.getValue();
        if (selectedDifficulty == null) { setFeedback("Please select a difficulty.", "feedback-error"); return; }
        int questionCount;
        try { questionCount = questionCountSpinner.getValue(); }
        catch (RuntimeException exception) { setFeedback("Invalid question count.", "feedback-error"); return; }
        if (questionCount <= 0) { setFeedback("Question count must be at least 1.", "feedback-error"); return; }
        sessionContext.setSelectedCategory(selected);
        sessionContext.setSelectedDifficulty(selectedDifficulty);
        sessionContext.setSelectedQuestionCount(questionCount);
        sessionContext.setLastQuizResult(null);
        setFeedback("", "feedback-error");
        screenManager.show(AppRoute.QUIZ);
    }

    private void logout() { sessionContext.clear(); screenManager.show(AppRoute.AUTH); }

    private int getDefaultQuestionCount() {
        try { return Math.max(1, Integer.parseInt(AppConfig.get("quiz.defaultQuestionCount"))); }
        catch (Exception e) { return 10; }
    }

    private void restoreCategorySelection(List<Category> categories) {
        Category selectedCategory = sessionContext.getSelectedCategory();
        if (selectedCategory != null) {
            categories.stream()
                    .filter(c -> c.getId() == selectedCategory.getId())
                    .findFirst()
                    .ifPresentOrElse(
                            c -> categoryListView.getSelectionModel().select(c),
                            () -> categoryListView.getSelectionModel().selectFirst()
                    );
            return;
        }
        if (categoryListView.getSelectionModel().getSelectedItem() == null) {
            categoryListView.getSelectionModel().selectFirst();
        }
    }

    private void updateQuestionAvailability() {
        Category selectedCategory = categoryListView.getSelectionModel().getSelectedItem();
        Difficulty selectedDifficulty = difficultyComboBox.getValue();
        if (selectedCategory == null || selectedDifficulty == null) {
            continueButton.setDisable(true);
            availabilityLabel.setText("Select a category and difficulty.");
            updateQuestionCountBounds(1);
            return;
        }
        try {
            List<Question> questions = questionDao.findByCategory(selectedCategory.getId());
            int total = questions.size();
            long diffCount = questions.stream().filter(q -> q.getDifficulty() == selectedDifficulty).count();
            updateQuestionCountBounds(total);
            continueButton.setDisable(total == 0);
            if (total == 0) {
                availabilityLabel.setText("⚠  No questions in this category yet.");
                setFeedback("This category has no questions. An admin needs to add some first.", "feedback-warning");
                return;
            }
            setFeedback("", "feedback-error");
            String diffText = diffCount == 0
                    ? "none with " + selectedDifficulty + " — will use other difficulties"
                    : diffCount + " with " + selectedDifficulty;
            availabilityLabel.setText("✅  " + total + " questions available  (" + diffText + ")");
        } catch (RuntimeException exception) {
            continueButton.setDisable(true);
            availabilityLabel.setText("Could not load question info.");
            setFeedback("Could not load question availability. Please refresh.", "feedback-error");
        }
    }

    private void updateQuestionCountBounds(int availableQuestions) {
        int upper = Math.max(1, Math.min(MAX_REQUESTABLE_QUESTIONS, availableQuestions));
        int current = questionCountSpinner.getValue() == null ? 1 : questionCountSpinner.getValue();
        int bounded = Math.min(Math.max(1, current), upper);
        questionCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, upper, bounded));
        questionCountSpinner.setEditable(true);
    }

    private void commitSpinnerValue() {
        String text = questionCountSpinner.getEditor().getText();
        if (text == null || text.isBlank()) return;
        try {
            int parsed = Integer.parseInt(text.trim());
            SpinnerValueFactory.IntegerSpinnerValueFactory f = (SpinnerValueFactory.IntegerSpinnerValueFactory) questionCountSpinner.getValueFactory();
            f.setValue(Math.min(f.getMax(), Math.max(f.getMin(), parsed)));
        } catch (NumberFormatException ignored) {}
    }

    private void setFeedback(String message, String styleClass) {
        feedbackLabel.getStyleClass().removeAll("feedback-error", "feedback-success", "feedback-warning");
        feedbackLabel.getStyleClass().add(styleClass);
        feedbackLabel.setText(message);
    }
}
