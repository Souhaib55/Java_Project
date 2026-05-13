package com.tekup.quiz.ui.screens;

import com.tekup.quiz.dao.CategoryDao;
import com.tekup.quiz.dao.QuestionDao;
import com.tekup.quiz.model.Category;
import com.tekup.quiz.model.Difficulty;
import com.tekup.quiz.model.McqQuestion;
import com.tekup.quiz.model.Question;
import com.tekup.quiz.model.QuestionType;
import com.tekup.quiz.model.Role;
import com.tekup.quiz.model.TrueFalseQuestion;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AdminQuestionScreen implements AppScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminQuestionScreen.class);

    private final ScreenManager screenManager;
    private final SessionContext sessionContext;
    private final QuestionDao questionDao;
    private final CategoryDao categoryDao;

    private final VBox root;
    private final Label subtitleLabel;
    private final Label feedbackLabel;
    private final ComboBox<CategoryFilter> filterCategoryCombo;
    private final ListView<Question> questionListView;

    private final ComboBox<QuestionType> typeCombo;
    private final ComboBox<Category> categoryCombo;
    private final ComboBox<Difficulty> difficultyCombo;
    private final TextArea promptArea;
    private final TextField optionAField;
    private final TextField optionBField;
    private final TextField optionCField;
    private final TextField optionDField;
    private final TextField correctAnswerField;
    private final Label formModeLabel;

    private final Map<Long, Category> categoryMap = new java.util.HashMap<>();
    private Question selectedQuestion;

    public AdminQuestionScreen(ScreenManager screenManager,
                               SessionContext sessionContext,
                               QuestionDao questionDao,
                               CategoryDao categoryDao) {
        this.screenManager = Objects.requireNonNull(screenManager);
        this.sessionContext = Objects.requireNonNull(sessionContext);
        this.questionDao = Objects.requireNonNull(questionDao);
        this.categoryDao = Objects.requireNonNull(categoryDao);

        // ── Header ────────────────────────────────────────────
        Label icon = new Label("❓");
        icon.setStyle("-fx-font-size: 24px;");
        Label title = new Label("Question Management");
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

        // ── Filter bar ────────────────────────────────────────
        Label filterLabel = new Label("Filter by category:");
        filterLabel.setStyle("-fx-text-fill: #7a8caa; -fx-font-size: 12px;");
        filterCategoryCombo = new ComboBox<>();
        filterCategoryCombo.setPrefWidth(240);
        filterCategoryCombo.setOnAction(event -> loadQuestions());

        HBox filterBar = new HBox(10, filterLabel, filterCategoryCombo);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        // ── Question list ─────────────────────────────────────
        Label listHeader = new Label("QUESTIONS");
        listHeader.setStyle("-fx-text-fill: #4a5a78; -fx-font-size: 11px; -fx-font-weight: 700;");

        questionListView = new ListView<>();
        questionListView.setPrefHeight(240);
        questionListView.getStyleClass().add("modern-list");
        questionListView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Question item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                String categoryName = categoryMap.containsKey(item.getCategoryId())
                        ? categoryMap.get(item.getCategoryId()).getName() : "?";
                String prompt = item.getPrompt();
                String shortPrompt = prompt.length() > 55 ? prompt.substring(0, 52) + "…" : prompt;

                String diffStyle = switch (item.getDifficulty()) {
                    case EASY -> "badge-easy";
                    case MEDIUM -> "badge-medium";
                    case HARD -> "badge-hard";
                };
                Label diffBadge = new Label(item.getDifficulty().name());
                diffBadge.getStyleClass().add(diffStyle);

                Label typeBadge = new Label(item.getType().name());
                typeBadge.getStyleClass().add("badge-info");

                Label catLabel = new Label(categoryName);
                catLabel.setStyle("-fx-text-fill: #7a8caa; -fx-font-size: 11px;");

                Label promptLabel = new Label(shortPrompt);
                promptLabel.setStyle("-fx-text-fill: #c7d2ee;");

                HBox badges = new HBox(6, typeBadge, diffBadge, catLabel);
                badges.setAlignment(Pos.CENTER_LEFT);

                VBox cellContent = new VBox(3, promptLabel, badges);
                setGraphic(cellContent);
                setText(null);
            }
        });
        questionListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> loadQuestionIntoForm(newVal));

        VBox listPane = new VBox(8, listHeader, filterBar, questionListView);
        VBox.setVgrow(questionListView, Priority.ALWAYS);

        // ── Form ──────────────────────────────────────────────
        Label formHeader = new Label("ADD / EDIT QUESTION");
        formHeader.setStyle("-fx-text-fill: #4a5a78; -fx-font-size: 11px; -fx-font-weight: 700;");

        formModeLabel = new Label("Fill in the fields to create a new question.");
        formModeLabel.setStyle("-fx-text-fill: #7a8caa; -fx-font-size: 12px;");
        formModeLabel.setWrapText(true);

        typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(QuestionType.MCQ, QuestionType.TRUE_FALSE);
        typeCombo.setValue(QuestionType.MCQ);
        typeCombo.setMaxWidth(Double.MAX_VALUE);
        typeCombo.setOnAction(event -> updateTypeInputs());

        // Category combo with explicit cell factory (belt-and-suspenders alongside toString())
        categoryCombo = new ComboBox<>();
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        categoryCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        categoryCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        difficultyCombo = new ComboBox<>();
        difficultyCombo.getItems().addAll(Difficulty.values());
        difficultyCombo.setValue(Difficulty.MEDIUM);
        difficultyCombo.setMaxWidth(Double.MAX_VALUE);

        promptArea = new TextArea();
        promptArea.setPromptText("Question text…");
        promptArea.setPrefRowCount(3);
        promptArea.setWrapText(true);
        promptArea.setMaxWidth(Double.MAX_VALUE);

        optionAField = new TextField(); optionAField.setPromptText("Option A");
        optionBField = new TextField(); optionBField.setPromptText("Option B");
        optionCField = new TextField(); optionCField.setPromptText("Option C");
        optionDField = new TextField(); optionDField.setPromptText("Option D");
        correctAnswerField = new TextField(); correctAnswerField.setPromptText("Correct (A/B/C/D or TRUE/FALSE)");

        GridPane optGrid = new GridPane();
        optGrid.setHgap(8); optGrid.setVgap(8);
        optGrid.add(optionAField, 0, 0); optGrid.add(optionBField, 1, 0);
        optGrid.add(optionCField, 0, 1); optGrid.add(optionDField, 1, 1);
        optGrid.getColumnConstraints().addAll(
                colConstraint(), colConstraint()
        );

        GridPane metaGrid = new GridPane();
        metaGrid.setHgap(10); metaGrid.setVgap(8);
        metaGrid.add(mkLabel("Type"), 0, 0); metaGrid.add(typeCombo, 1, 0);
        metaGrid.add(mkLabel("Category"), 0, 1); metaGrid.add(categoryCombo, 1, 1);
        metaGrid.add(mkLabel("Difficulty"), 0, 2); metaGrid.add(difficultyCombo, 1, 2);
        metaGrid.getColumnConstraints().addAll(fixedCol(80), colConstraint());

        Button saveButton = new Button("💾  Save Question");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setOnAction(event -> saveQuestion());

        Button deleteButton = new Button("🗑  Delete Selected");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setOnAction(event -> deleteQuestion());

        Button clearButton = new Button("✕  Clear Form");
        clearButton.getStyleClass().add("nav-button");
        clearButton.setMaxWidth(Double.MAX_VALUE);
        clearButton.setOnAction(event -> clearForm());

        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.getStyleClass().add("feedback-error");

        VBox formPane = new VBox(10,
                formHeader, formModeLabel,
                metaGrid,
                mkLabel("Question Prompt"), promptArea,
                mkLabel("Options (MCQ)"), optGrid,
                new VBox(5, mkLabel("Correct Answer"), correctAnswerField),
                saveButton, deleteButton, clearButton,
                feedbackLabel
        );
        formPane.getStyleClass().add("stat-card");
        formPane.setPrefWidth(380);

        HBox contentRow = new HBox(20, listPane, formPane);
        HBox.setHgrow(listPane, Priority.ALWAYS);
        contentRow.setAlignment(Pos.TOP_LEFT);

        root = new VBox(16, titleRow, subtitleLabel, contentRow);
        root.getStyleClass().addAll("app-root", "screen-admin");
        root.setPadding(new Insets(28));

        updateTypeInputs();
    }

    private Label mkLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #7a8caa; -fx-font-size: 12px; -fx-font-weight: 600;");
        return l;
    }

    private javafx.scene.layout.ColumnConstraints colConstraint() {
        javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
        cc.setHgrow(Priority.ALWAYS);
        cc.setFillWidth(true);
        return cc;
    }

    private javafx.scene.layout.ColumnConstraints fixedCol(double w) {
        javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints(w);
        cc.setHgrow(Priority.NEVER);
        return cc;
    }

    @Override public String title() { return "QuizMaster — Questions"; }
    @Override public Parent root() { return root; }

    @Override
    public void onShow() {
        User currentUser = sessionContext.getCurrentUser();
        if (currentUser == null) { screenManager.show(AppRoute.AUTH); return; }
        if (currentUser.getRole() != Role.ADMIN) { screenManager.show(AppRoute.CATEGORY); return; }
        subtitleLabel.setText("Create, edit and delete MCQ / True-False questions");
        reloadCategories();
        loadQuestions();
        setFeedback("", "feedback-error");
    }

    private void reloadCategories() {
        try {
            List<Category> categories = categoryDao.findAll();
            categoryMap.clear();
            categoryMap.putAll(categories.stream().collect(Collectors.toMap(Category::getId, Function.identity())));
            categoryCombo.setItems(FXCollections.observableArrayList(categories));
            if (!categories.isEmpty() && categoryCombo.getValue() == null) {
                categoryCombo.setValue(categories.get(0));
            }
            List<CategoryFilter> filterOptions = new ArrayList<>();
            filterOptions.add(CategoryFilter.all());
            for (Category category : categories) filterOptions.add(new CategoryFilter(category.getId(), category.getName()));
            filterCategoryCombo.setItems(FXCollections.observableArrayList(filterOptions));
            if (filterCategoryCombo.getValue() == null) filterCategoryCombo.setValue(filterOptions.get(0));
        } catch (RuntimeException exception) {
            setFeedback("Could not load categories.", "feedback-error");
        }
    }

    private void loadQuestions() {
        try {
            CategoryFilter selectedFilter = filterCategoryCombo.getValue();
            List<Question> questions;
            if (selectedFilter == null || selectedFilter.categoryId() == null) {
                questions = questionDao.findAll();
            } else {
                questions = questionDao.findByCategory(selectedFilter.categoryId());
            }
            questionListView.setItems(FXCollections.observableArrayList(questions));
            if (questions.isEmpty()) setFeedback("No questions for this filter.", "feedback-warning");
            else setFeedback("", "feedback-error");
        } catch (RuntimeException exception) {
            setFeedback("Could not load questions.", "feedback-error");
        }
    }

    private void saveQuestion() {
        User actor = sessionContext.getCurrentUser();
        long actorId = actor == null ? -1 : actor.getId();
        try {
            QuestionType type = typeCombo.getValue();
            Category category = categoryCombo.getValue();
            Difficulty difficulty = difficultyCombo.getValue();
            String prompt = FormValidator.validatePrompt(promptArea.getText());
            if (type == null || category == null || difficulty == null) {
                setFeedback("Type, category and difficulty are required.", "feedback-error"); return;
            }
            Question question;
            boolean updateMode = selectedQuestion != null;
            if (type == QuestionType.MCQ) {
                String optA = FormValidator.validateOption("Option A", optionAField.getText());
                String optB = FormValidator.validateOption("Option B", optionBField.getText());
                String optC = FormValidator.validateOption("Option C", optionCField.getText());
                String optD = FormValidator.validateOption("Option D", optionDField.getText());
                FormValidator.validateDistinctOptions(optA, optB, optC, optD);
                String correct = FormValidator.validateMcqCorrectAnswer(correctAnswerField.getText());
                question = selectedQuestion != null
                        ? new McqQuestion(selectedQuestion.getId(), category.getId(), difficulty, prompt, optA, optB, optC, optD, correct)
                        : new McqQuestion(category.getId(), difficulty, prompt, optA, optB, optC, optD, correct);
            } else {
                String correct = FormValidator.validateTrueFalseCorrectAnswer(correctAnswerField.getText());
                question = selectedQuestion != null
                        ? new TrueFalseQuestion(selectedQuestion.getId(), category.getId(), difficulty, prompt, correct)
                        : new TrueFalseQuestion(category.getId(), difficulty, prompt, correct);
            }
            Question saved = questionDao.save(question);
            LOGGER.info("Admin question upsert: actorId={} mode={} questionId={}", actorId, updateMode ? "update" : "create", saved.getId());
            clearForm();
            loadQuestions();
            setFeedback("✅  Question saved.", "feedback-success");
        } catch (IllegalArgumentException exception) {
            setFeedback(exception.getMessage(), "feedback-error");
        } catch (RuntimeException exception) {
            LOGGER.error("Admin question upsert failed: actorId={}", actorId, exception);
            setFeedback("Could not save question: " + exception.getMessage(), "feedback-error");
        }
    }

    private void deleteQuestion() {
        User actor = sessionContext.getCurrentUser();
        long actorId = actor == null ? -1 : actor.getId();
        if (selectedQuestion == null) { setFeedback("Select a question first.", "feedback-error"); return; }
        try {
            boolean deleted = questionDao.deleteById(selectedQuestion.getId());
            if (!deleted) { setFeedback("Question not found or already deleted.", "feedback-error"); return; }
            LOGGER.info("Admin question delete: actorId={} questionId={}", actorId, selectedQuestion.getId());
            clearForm();
            loadQuestions();
            setFeedback("🗑  Question deleted.", "feedback-success");
        } catch (RuntimeException exception) {
            LOGGER.error("Admin question delete failed: actorId={} questionId={}", actorId, selectedQuestion.getId(), exception);
            setFeedback("Could not delete question.", "feedback-error");
        }
    }

    private void loadQuestionIntoForm(Question question) {
        selectedQuestion = question;
        if (question == null) return;
        typeCombo.setValue(question.getType());
        difficultyCombo.setValue(question.getDifficulty());
        promptArea.setText(question.getPrompt());
        correctAnswerField.setText(question.getCorrectAnswer());
        Category category = categoryMap.get(question.getCategoryId());
        if (category != null) categoryCombo.setValue(category);
        if (question instanceof McqQuestion mcq) {
            optionAField.setText(mcq.getOptionA());
            optionBField.setText(mcq.getOptionB());
            optionCField.setText(mcq.getOptionC());
            optionDField.setText(mcq.getOptionD());
        } else {
            optionAField.clear(); optionBField.clear(); optionCField.clear(); optionDField.clear();
        }
        updateTypeInputs();
        formModeLabel.setText("✏  Editing question #" + question.getId());
        formModeLabel.setStyle("-fx-text-fill: #f5a623; -fx-font-size: 12px; -fx-font-weight: 600;");
    }

    private void clearForm() {
        selectedQuestion = null;
        questionListView.getSelectionModel().clearSelection();
        typeCombo.setValue(QuestionType.MCQ);
        difficultyCombo.setValue(Difficulty.MEDIUM);
        promptArea.clear();
        optionAField.clear(); optionBField.clear(); optionCField.clear(); optionDField.clear();
        correctAnswerField.clear();
        formModeLabel.setText("Fill in the fields to create a new question.");
        formModeLabel.setStyle("-fx-text-fill: #7a8caa; -fx-font-size: 12px;");
        updateTypeInputs();
    }

    private void updateTypeInputs() {
        boolean isMcq = typeCombo.getValue() == QuestionType.MCQ;
        optionAField.setDisable(!isMcq); optionBField.setDisable(!isMcq);
        optionCField.setDisable(!isMcq); optionDField.setDisable(!isMcq);
        if (!isMcq) {
            optionAField.clear(); optionBField.clear(); optionCField.clear(); optionDField.clear();
            correctAnswerField.setPromptText("TRUE or FALSE");
        } else {
            correctAnswerField.setPromptText("A, B, C or D");
        }
    }

    private record CategoryFilter(Long categoryId, String label) {
        private static CategoryFilter all() { return new CategoryFilter(null, "All categories"); }
        @Override public String toString() { return label; }
    }

    private void setFeedback(String message, String styleClass) {
        feedbackLabel.getStyleClass().removeAll("feedback-error", "feedback-success", "feedback-warning");
        feedbackLabel.getStyleClass().add(styleClass);
        feedbackLabel.setText(message);
    }
}
