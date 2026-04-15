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

    private final Map<Long, Category> categoryMap = new java.util.HashMap<>();
    private Question selectedQuestion;

    public AdminQuestionScreen(ScreenManager screenManager,
                               SessionContext sessionContext,
                               QuestionDao questionDao,
                               CategoryDao categoryDao) {
        this.screenManager = Objects.requireNonNull(screenManager, "screenManager must not be null");
        this.sessionContext = Objects.requireNonNull(sessionContext, "sessionContext must not be null");
        this.questionDao = Objects.requireNonNull(questionDao, "questionDao must not be null");
        this.categoryDao = Objects.requireNonNull(categoryDao, "categoryDao must not be null");

        Label title = new Label("Question Management");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        subtitleLabel = new Label();
        subtitleLabel.setStyle("-fx-text-fill: #2f4050;");

        filterCategoryCombo = new ComboBox<>();
        filterCategoryCombo.setPrefWidth(260);
        filterCategoryCombo.setOnAction(event -> loadQuestions());

        questionListView = new ListView<>();
        questionListView.setPrefHeight(260);
        questionListView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Question item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String categoryName = categoryMap.containsKey(item.getCategoryId())
                        ? categoryMap.get(item.getCategoryId()).getName()
                        : "Unknown";
                String prompt = item.getPrompt();
                String shortPrompt = prompt.length() > 60 ? prompt.substring(0, 57) + "..." : prompt;
                setText("#" + item.getId() + " " + item.getType() + " | " + categoryName + " | " + item.getDifficulty() + " | " + shortPrompt);
            }
        });
        questionListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> loadQuestionIntoForm(newVal));

        typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(QuestionType.MCQ, QuestionType.TRUE_FALSE);
        typeCombo.setValue(QuestionType.MCQ);
        typeCombo.setOnAction(event -> updateTypeInputs());

        categoryCombo = new ComboBox<>();

        difficultyCombo = new ComboBox<>();
        difficultyCombo.getItems().addAll(Difficulty.values());
        difficultyCombo.setValue(Difficulty.MEDIUM);

        promptArea = new TextArea();
        promptArea.setPromptText("Question prompt");
        promptArea.setPrefRowCount(3);

        optionAField = new TextField();
        optionAField.setPromptText("Option A");

        optionBField = new TextField();
        optionBField.setPromptText("Option B");

        optionCField = new TextField();
        optionCField.setPromptText("Option C");

        optionDField = new TextField();
        optionDField.setPromptText("Option D");

        correctAnswerField = new TextField();
        correctAnswerField.setPromptText("Correct answer (A/B/C/D or TRUE/FALSE)");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(8);
        formGrid.setVgap(8);
        formGrid.add(new Label("Type"), 0, 0);
        formGrid.add(typeCombo, 1, 0);
        formGrid.add(new Label("Category"), 0, 1);
        formGrid.add(categoryCombo, 1, 1);
        formGrid.add(new Label("Difficulty"), 0, 2);
        formGrid.add(difficultyCombo, 1, 2);
        formGrid.add(new Label("Prompt"), 0, 3);
        formGrid.add(promptArea, 1, 3);
        formGrid.add(new Label("Option A"), 0, 4);
        formGrid.add(optionAField, 1, 4);
        formGrid.add(new Label("Option B"), 0, 5);
        formGrid.add(optionBField, 1, 5);
        formGrid.add(new Label("Option C"), 0, 6);
        formGrid.add(optionCField, 1, 6);
        formGrid.add(new Label("Option D"), 0, 7);
        formGrid.add(optionDField, 1, 7);
        formGrid.add(new Label("Correct"), 0, 8);
        formGrid.add(correctAnswerField, 1, 8);

        Button saveButton = new Button("Save question");
        saveButton.setOnAction(event -> saveQuestion());

        Button deleteButton = new Button("Delete selected");
        deleteButton.setOnAction(event -> deleteQuestion());

        Button clearButton = new Button("Clear form");
        clearButton.setOnAction(event -> clearForm());

        Button backButton = new Button("Back to dashboard");
        backButton.setOnAction(event -> screenManager.show(AppRoute.ADMIN_DASHBOARD));

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> {
            sessionContext.clear();
            screenManager.show(AppRoute.AUTH);
        });

        HBox filterBar = new HBox(8, new Label("Filter"), filterCategoryCombo);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        HBox actions = new HBox(10, saveButton, deleteButton, clearButton, backButton, logoutButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setStyle("-fx-text-fill: #b00020;");

        root = new VBox(10, title, subtitleLabel, filterBar, questionListView, formGrid, actions, feedbackLabel);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #eef4fa;");

        updateTypeInputs();
    }

    @Override
    public String title() {
        return "Interactive Quiz - Admin Questions";
    }

    @Override
    public Parent root() {
        return root;
    }

    @Override
    public void onShow() {
        User currentUser = sessionContext.getCurrentUser();
        if (currentUser == null) {
            LOGGER.warn("Admin question access denied: no authenticated user");
            screenManager.show(AppRoute.AUTH);
            return;
        }
        if (currentUser.getRole() != Role.ADMIN) {
            LOGGER.warn("Admin question access denied: userId={} role={}", currentUser.getId(), currentUser.getRole());
            screenManager.show(AppRoute.CATEGORY);
            return;
        }

        subtitleLabel.setText("Create, edit, and delete MCQ / True-False questions");
        reloadCategories();
        loadQuestions();
        feedbackLabel.setText("");
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
            for (Category category : categories) {
                filterOptions.add(new CategoryFilter(category.getId(), category.getName()));
            }
            filterCategoryCombo.setItems(FXCollections.observableArrayList(filterOptions));
            if (filterCategoryCombo.getValue() == null) {
                filterCategoryCombo.setValue(filterOptions.get(0));
            }
        } catch (RuntimeException exception) {
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Could not load categories.");
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
            if (questions.isEmpty()) {
                feedbackLabel.setStyle("-fx-text-fill: #7a6200;");
                feedbackLabel.setText("No questions available for current filter.");
            }
        } catch (RuntimeException exception) {
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Could not load questions.");
        }
    }

    private void saveQuestion() {
        User actor = sessionContext.getCurrentUser();
        String actorName = actor == null ? "unknown" : actor.getUsername();
        long actorId = actor == null ? -1 : actor.getId();

        try {
            QuestionType type = typeCombo.getValue();
            Category category = categoryCombo.getValue();
            Difficulty difficulty = difficultyCombo.getValue();
            String prompt = FormValidator.validatePrompt(promptArea.getText());

            if (type == null || category == null || difficulty == null) {
                feedbackLabel.setStyle("-fx-text-fill: #b00020;");
                feedbackLabel.setText("Type, category, difficulty and prompt are required.");
                return;
            }

            Question question;
            boolean updateMode = selectedQuestion != null;
            if (type == QuestionType.MCQ) {
                String optionA = FormValidator.validateOption("Option A", optionAField.getText());
                String optionB = FormValidator.validateOption("Option B", optionBField.getText());
                String optionC = FormValidator.validateOption("Option C", optionCField.getText());
                String optionD = FormValidator.validateOption("Option D", optionDField.getText());
                FormValidator.validateDistinctOptions(optionA, optionB, optionC, optionD);
                String correct = FormValidator.validateMcqCorrectAnswer(correctAnswerField.getText());

                if (selectedQuestion != null) {
                    question = new McqQuestion(
                            selectedQuestion.getId(),
                            category.getId(),
                            difficulty,
                            prompt,
                            optionA,
                            optionB,
                            optionC,
                            optionD,
                            correct
                    );
                } else {
                    question = new McqQuestion(category.getId(), difficulty, prompt, optionA, optionB, optionC, optionD, correct);
                }
            } else {
                String correct = FormValidator.validateTrueFalseCorrectAnswer(correctAnswerField.getText());
                if (selectedQuestion != null) {
                    question = new TrueFalseQuestion(
                            selectedQuestion.getId(),
                            category.getId(),
                            difficulty,
                            prompt,
                            correct
                    );
                } else {
                    question = new TrueFalseQuestion(category.getId(), difficulty, prompt, correct);
                }
            }

            Question saved = questionDao.save(question);
            LOGGER.info(
                    "Admin question upsert successful: actorId={} actor={} mode={} questionId={} type={} categoryId={} difficulty={}",
                    actorId,
                    actorName,
                    updateMode ? "update" : "create",
                    saved.getId(),
                    saved.getType(),
                    saved.getCategoryId(),
                    saved.getDifficulty()
            );

            feedbackLabel.setStyle("-fx-text-fill: #0a7d2f;");
            feedbackLabel.setText("Question saved.");
            clearForm();
            loadQuestions();
        } catch (IllegalArgumentException exception) {
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText(exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.error("Admin question upsert failed: actorId={} actor={}", actorId, actorName, exception);
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Could not save question: " + exception.getMessage());
        }
    }

    private void deleteQuestion() {
        User actor = sessionContext.getCurrentUser();
        String actorName = actor == null ? "unknown" : actor.getUsername();
        long actorId = actor == null ? -1 : actor.getId();

        if (selectedQuestion == null) {
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Select a question to delete.");
            return;
        }

        try {
            boolean deleted = questionDao.deleteById(selectedQuestion.getId());
            if (!deleted) {
                feedbackLabel.setStyle("-fx-text-fill: #b00020;");
                feedbackLabel.setText("Question was not deleted.");
                return;
            }

            LOGGER.info(
                    "Admin question delete successful: actorId={} actor={} questionId={} type={} categoryId={}",
                    actorId,
                    actorName,
                    selectedQuestion.getId(),
                    selectedQuestion.getType(),
                    selectedQuestion.getCategoryId()
            );

            feedbackLabel.setStyle("-fx-text-fill: #0a7d2f;");
            feedbackLabel.setText("Question deleted.");
            clearForm();
            loadQuestions();
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Admin question delete failed: actorId={} actor={} questionId={}",
                    actorId,
                    actorName,
                    selectedQuestion.getId(),
                    exception
            );
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Could not delete question.");
        }
    }

    private void loadQuestionIntoForm(Question question) {
        selectedQuestion = question;
        if (question == null) {
            return;
        }

        typeCombo.setValue(question.getType());
        difficultyCombo.setValue(question.getDifficulty());
        promptArea.setText(question.getPrompt());
        correctAnswerField.setText(question.getCorrectAnswer());

        Category category = categoryMap.get(question.getCategoryId());
        if (category != null) {
            categoryCombo.setValue(category);
        }

        if (question instanceof McqQuestion mcq) {
            optionAField.setText(mcq.getOptionA());
            optionBField.setText(mcq.getOptionB());
            optionCField.setText(mcq.getOptionC());
            optionDField.setText(mcq.getOptionD());
        } else {
            optionAField.clear();
            optionBField.clear();
            optionCField.clear();
            optionDField.clear();
        }

        updateTypeInputs();
    }

    private void clearForm() {
        selectedQuestion = null;
        questionListView.getSelectionModel().clearSelection();
        typeCombo.setValue(QuestionType.MCQ);
        difficultyCombo.setValue(Difficulty.MEDIUM);
        promptArea.clear();
        optionAField.clear();
        optionBField.clear();
        optionCField.clear();
        optionDField.clear();
        correctAnswerField.clear();
        updateTypeInputs();
    }

    private void updateTypeInputs() {
        boolean isMcq = typeCombo.getValue() == QuestionType.MCQ;
        optionAField.setDisable(!isMcq);
        optionBField.setDisable(!isMcq);
        optionCField.setDisable(!isMcq);
        optionDField.setDisable(!isMcq);

        if (!isMcq) {
            optionAField.clear();
            optionBField.clear();
            optionCField.clear();
            optionDField.clear();
            correctAnswerField.setPromptText("Correct answer (TRUE/FALSE)");
        } else {
            correctAnswerField.setPromptText("Correct answer (A/B/C/D)");
        }
    }

    private record CategoryFilter(Long categoryId, String label) {
        private static CategoryFilter all() {
            return new CategoryFilter(null, "All categories");
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
