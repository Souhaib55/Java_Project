package com.tekup.quiz;

import com.tekup.quiz.dao.CategoryDao;
import com.tekup.quiz.dao.QuestionDao;
import com.tekup.quiz.dao.QuizAttemptDao;
import com.tekup.quiz.dao.UserDao;
import com.tekup.quiz.dao.jdbc.JdbcCategoryDao;
import com.tekup.quiz.dao.jdbc.JdbcQuestionDao;
import com.tekup.quiz.dao.jdbc.JdbcQuizAttemptDao;
import com.tekup.quiz.dao.jdbc.JdbcUserDao;
import com.tekup.quiz.service.AuthService;
import com.tekup.quiz.service.CsvQuestionService;
import com.tekup.quiz.service.HistoryService;
import com.tekup.quiz.service.LeaderboardService;
import com.tekup.quiz.service.QuizService;
import com.tekup.quiz.service.ResultExportService;
import com.tekup.quiz.ui.AppRoute;
import com.tekup.quiz.ui.ScreenManager;
import com.tekup.quiz.ui.SessionContext;
import com.tekup.quiz.ui.screens.AdminCategoryScreen;
import com.tekup.quiz.ui.screens.AdminCsvScreen;
import com.tekup.quiz.ui.screens.AdminDashboardScreen;
import com.tekup.quiz.ui.screens.AdminQuestionScreen;
import com.tekup.quiz.ui.screens.AuthScreen;
import com.tekup.quiz.ui.screens.CategorySelectionScreen;
import com.tekup.quiz.ui.screens.HistoryScreen;
import com.tekup.quiz.ui.screens.LeaderboardScreen;
import com.tekup.quiz.ui.screens.QuizScreen;
import com.tekup.quiz.ui.screens.ResultScreen;
import com.tekup.quiz.util.DatabaseManager;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        try {
            DatabaseManager.verifyConnection();

            UserDao userDao = new JdbcUserDao();
            CategoryDao categoryDao = new JdbcCategoryDao();
            QuestionDao questionDao = new JdbcQuestionDao();
            QuizAttemptDao quizAttemptDao = new JdbcQuizAttemptDao();

            AuthService authService = new AuthService(userDao);
            QuizService quizService = new QuizService(questionDao, quizAttemptDao);
            LeaderboardService leaderboardService = new LeaderboardService(quizAttemptDao);
            HistoryService historyService = new HistoryService(quizAttemptDao);
            CsvQuestionService csvQuestionService = new CsvQuestionService(
                    questionDao,
                    categoryId -> categoryDao.findById(categoryId).isPresent()
            );
            ResultExportService resultExportService = new ResultExportService();

            SessionContext sessionContext = new SessionContext();
            ScreenManager screenManager = new ScreenManager(stage, sessionContext);

            screenManager.register(
                    AppRoute.AUTH,
                    new AuthScreen(screenManager, sessionContext, authService)
            );
            screenManager.register(
                    AppRoute.CATEGORY,
                    new CategorySelectionScreen(screenManager, sessionContext, categoryDao)
            );
            screenManager.register(
                    AppRoute.QUIZ,
                    new QuizScreen(screenManager, sessionContext, quizService)
            );
            screenManager.register(
                    AppRoute.RESULT,
                    new ResultScreen(screenManager, sessionContext, resultExportService)
            );
            screenManager.register(
                    AppRoute.LEADERBOARD,
                    new LeaderboardScreen(screenManager, sessionContext, leaderboardService, categoryDao)
            );
            screenManager.register(
                    AppRoute.HISTORY,
                    new HistoryScreen(screenManager, sessionContext, historyService)
            );
            screenManager.register(
                    AppRoute.ADMIN_DASHBOARD,
                    new AdminDashboardScreen(screenManager, sessionContext)
            );
            screenManager.register(
                    AppRoute.ADMIN_CATEGORY,
                    new AdminCategoryScreen(screenManager, sessionContext, categoryDao)
            );
            screenManager.register(
                    AppRoute.ADMIN_QUESTION,
                    new AdminQuestionScreen(screenManager, sessionContext, questionDao, categoryDao)
            );
            screenManager.register(
                    AppRoute.ADMIN_CSV,
                    new AdminCsvScreen(screenManager, sessionContext, csvQuestionService, questionDao)
            );

            screenManager.show(AppRoute.AUTH);
        } catch (RuntimeException exception) {
                        DatabaseManager.shutdown();
            showStartupError(stage, exception);
        }
    }

        @Override
        public void stop() {
                DatabaseManager.shutdown();
        }

    private void showStartupError(Stage stage, RuntimeException exception) {
        Label titleLabel = new Label("Application startup failed");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label helpLabel = new Label(
                "Check database availability and configuration keys db.url, db.username, and db.password."
        );
        helpLabel.setWrapText(true);

        TextArea detailArea = new TextArea(resolveStartupDetail(exception));
        detailArea.setWrapText(true);
        detailArea.setEditable(false);
        detailArea.setPrefRowCount(4);

        Button closeButton = new Button("Close");
        closeButton.setOnAction(event -> stage.close());

        VBox root = new VBox(12, titleLabel, helpLabel, detailArea, closeButton);
        root.setAlignment(Pos.TOP_LEFT);
        root.setPadding(new Insets(16));

        stage.setTitle("Interactive Quiz - Startup Error");
        stage.setScene(new Scene(root, 780, 300));
        stage.show();
    }

    private String resolveStartupDetail(RuntimeException exception) {
        Throwable root = exception;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getMessage();
        }
        if (message == null || message.isBlank()) {
            message = "Unknown startup failure.";
        }
        return message;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
