package com.tekup.quiz.ui.screens;

import com.tekup.quiz.model.Category;
import com.tekup.quiz.model.Difficulty;
import com.tekup.quiz.model.Question;
import com.tekup.quiz.model.QuestionType;
import com.tekup.quiz.model.QuizResult;
import com.tekup.quiz.model.User;
import com.tekup.quiz.service.QuizService;
import com.tekup.quiz.ui.AppRoute;
import com.tekup.quiz.ui.AppScreen;
import com.tekup.quiz.ui.ScreenManager;
import com.tekup.quiz.ui.SessionContext;
import com.tekup.quiz.util.AppConfig;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class QuizScreen implements AppScreen {
    private static final String[] MCQ_CODES = {"A", "B", "C", "D"};

    private final ScreenManager screenManager;
    private final SessionContext sessionContext;
    private final QuizService quizService;

    private final VBox root;
    private final Label setupLabel;
    private final Label progressLabel;
    private final Label timerLabel;
    private final Label questionLabel;
    private final Label feedbackLabel;
    private final VBox optionsContainer;
    private final Button submitButton;
    private final Button skipButton;

    private List<Question> questions = List.of();
    private List<String> submittedAnswers = List.of();
    private ToggleGroup answerToggleGroup = new ToggleGroup();
    private Timeline timer;
    private int secondsPerQuestion;
    private int remainingSeconds;
    private int currentQuestionIndex;
    private boolean evaluating;

    public QuizScreen(ScreenManager screenManager, SessionContext sessionContext, QuizService quizService) {
        this.screenManager = Objects.requireNonNull(screenManager, "screenManager must not be null");
        this.sessionContext = Objects.requireNonNull(sessionContext, "sessionContext must not be null");
        this.quizService = Objects.requireNonNull(quizService, "quizService must not be null");

        Label title = new Label("Quiz");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        setupLabel = new Label();
        setupLabel.setStyle("-fx-text-fill: #2f4050;");

        progressLabel = new Label();
        progressLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #4a5a6a;");

        timerLabel = new Label();
        timerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0b4d9b;");

        questionLabel = new Label();
        questionLabel.setWrapText(true);
        questionLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");

        optionsContainer = new VBox(10);

        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setStyle("-fx-text-fill: #b00020;");

        submitButton = new Button("Submit answer");
        submitButton.setOnAction(event -> submitAndNext());

        skipButton = new Button("Skip");
        skipButton.setOnAction(event -> skipAndNext());

        Button cancelButton = new Button("Cancel quiz");
        cancelButton.setOnAction(event -> {
            stopTimer();
            screenManager.show(AppRoute.CATEGORY);
        });

        HBox actions = new HBox(10, submitButton, skipButton, cancelButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        root = new VBox(14, title, setupLabel, progressLabel, timerLabel, questionLabel, optionsContainer, actions, feedbackLabel);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f2f8fb;");
    }

    @Override
    public String title() {
        return "Interactive Quiz - Quiz";
    }

    @Override
    public Parent root() {
        return root;
    }

    @Override
    public void onShow() {
        feedbackLabel.setText("");
        evaluating = false;

        User user = sessionContext.getCurrentUser();
        Category category = sessionContext.getSelectedCategory();
        Difficulty difficulty = sessionContext.getSelectedDifficulty();
        Integer questionCount = sessionContext.getSelectedQuestionCount();

        if (user == null) {
            screenManager.show(AppRoute.AUTH);
            return;
        }
        if (category == null || difficulty == null || questionCount == null || questionCount <= 0) {
            screenManager.show(AppRoute.CATEGORY);
            return;
        }

        secondsPerQuestion = readSecondsPerQuestion();
        setupLabel.setText(
                "Player: " + user.getUsername() +
                        " | Category: " + category.getName() +
                        " | Difficulty: " + difficulty +
                        " | Questions: " + questionCount
        );

        loadQuiz(category.getId(), difficulty, questionCount);
    }

    @Override
    public void onHide() {
        stopTimer();
    }

    private void loadQuiz(long categoryId, Difficulty difficulty, int questionCount) {
        submitButton.setDisable(false);
        skipButton.setDisable(false);
        try {
            questions = quizService.generateQuiz(categoryId, difficulty, questionCount);
            submittedAnswers = new ArrayList<>(Collections.nCopies(questions.size(), ""));
            currentQuestionIndex = 0;
            renderCurrentQuestion();

            if (questions.size() < questionCount) {
                feedbackLabel.setStyle("-fx-text-fill: #7a6200;");
                feedbackLabel.setText("Only " + questions.size() + " question(s) were available for this setup.");
            }
        } catch (IllegalStateException exception) {
            if (exception.getMessage() != null
                    && exception.getMessage().toLowerCase(Locale.ROOT).contains("no questions are available")) {
                optionsContainer.getChildren().clear();
                questionLabel.setText("No questions are available for this selection.");
                progressLabel.setText("Question 0/0");
                timerLabel.setText("Timer: --");
                feedbackLabel.setStyle("-fx-text-fill: #7a6200;");
                feedbackLabel.setText("Try another category or add questions from admin tools.");
                submitButton.setDisable(true);
                skipButton.setDisable(true);
                return;
            }

            optionsContainer.getChildren().clear();
            questionLabel.setText("Could not start quiz.");
            progressLabel.setText("Question 0/0");
            timerLabel.setText("Timer: --");
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Failed to load questions. Check database connectivity.");
            submitButton.setDisable(true);
            skipButton.setDisable(true);
        } catch (RuntimeException exception) {
            optionsContainer.getChildren().clear();
            questionLabel.setText("Could not start quiz.");
            progressLabel.setText("Question 0/0");
            timerLabel.setText("Timer: --");
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Failed to load questions. Check database connectivity.");
            submitButton.setDisable(true);
            skipButton.setDisable(true);
        }
    }

    private void renderCurrentQuestion() {
        if (currentQuestionIndex >= questions.size()) {
            evaluateQuiz();
            return;
        }

        Question currentQuestion = questions.get(currentQuestionIndex);
        progressLabel.setText("Question " + (currentQuestionIndex + 1) + "/" + questions.size());
        questionLabel.setText(currentQuestion.getPrompt());

        answerToggleGroup = new ToggleGroup();
        optionsContainer.getChildren().clear();

        List<String> options = currentQuestion.getOptions();
        for (int optionIndex = 0; optionIndex < options.size(); optionIndex++) {
            String code = optionCode(currentQuestion, optionIndex);
            String text = optionText(currentQuestion, optionIndex, options.get(optionIndex));

            RadioButton optionButton = new RadioButton(text);
            optionButton.setUserData(code);
            optionButton.setToggleGroup(answerToggleGroup);
            optionButton.setWrapText(true);
            optionsContainer.getChildren().add(optionButton);

            String currentSavedAnswer = submittedAnswers.get(currentQuestionIndex);
            if (code.equalsIgnoreCase(currentSavedAnswer)) {
                answerToggleGroup.selectToggle(optionButton);
            }
        }

        startTimer();
    }

    private String optionCode(Question question, int optionIndex) {
        if (question.getType() == QuestionType.MCQ) {
            return MCQ_CODES[optionIndex];
        }
        return question.getOptions().get(optionIndex).trim().toUpperCase(Locale.ROOT);
    }

    private String optionText(Question question, int optionIndex, String optionValue) {
        if (question.getType() == QuestionType.MCQ) {
            return MCQ_CODES[optionIndex] + ". " + optionValue;
        }
        return optionValue;
    }

    private void submitAndNext() {
        if (evaluating || questions.isEmpty()) {
            return;
        }
        storeCurrentAnswerFromSelection();
        moveToNextQuestion();
    }

    private void skipAndNext() {
        if (evaluating || questions.isEmpty()) {
            return;
        }
        submittedAnswers.set(currentQuestionIndex, "");
        moveToNextQuestion();
    }

    private void moveToNextQuestion() {
        stopTimer();
        currentQuestionIndex++;
        renderCurrentQuestion();
    }

    private void storeCurrentAnswerFromSelection() {
        Toggle selectedToggle = answerToggleGroup.getSelectedToggle();
        if (selectedToggle == null || selectedToggle.getUserData() == null) {
            submittedAnswers.set(currentQuestionIndex, "");
            return;
        }
        submittedAnswers.set(currentQuestionIndex, selectedToggle.getUserData().toString());
    }

    private void startTimer() {
        stopTimer();
        remainingSeconds = Math.max(1, secondsPerQuestion);
        timerLabel.setText("Timer: " + remainingSeconds + "s");

        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingSeconds--;
            timerLabel.setText("Timer: " + Math.max(remainingSeconds, 0) + "s");
            if (remainingSeconds <= 0) {
                submittedAnswers.set(currentQuestionIndex, "");
                feedbackLabel.setStyle("-fx-text-fill: #7a6200;");
                feedbackLabel.setText("Time expired for question " + (currentQuestionIndex + 1) + ". Marked as incorrect.");
                moveToNextQuestion();
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.playFromStart();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    private void evaluateQuiz() {
        if (evaluating) {
            return;
        }
        evaluating = true;
        stopTimer();
        submitButton.setDisable(true);
        skipButton.setDisable(true);

        try {
            User user = sessionContext.getCurrentUser();
            Category category = sessionContext.getSelectedCategory();
            QuizResult result = quizService.evaluate(user.getId(), category.getId(), questions, submittedAnswers);
            sessionContext.setLastQuizResult(result);
            screenManager.show(AppRoute.RESULT);
        } catch (RuntimeException exception) {
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Could not finalize quiz. Please try again.");
            submitButton.setDisable(false);
            skipButton.setDisable(false);
            evaluating = false;
        }
    }

    private int readSecondsPerQuestion() {
        try {
            return Integer.parseInt(AppConfig.get("quiz.secondsPerQuestion"));
        } catch (Exception exception) {
            return 15;
        }
    }
}
