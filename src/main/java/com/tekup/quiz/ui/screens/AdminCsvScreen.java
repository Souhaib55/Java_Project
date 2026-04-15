package com.tekup.quiz.ui.screens;

import com.tekup.quiz.dao.QuestionDao;
import com.tekup.quiz.model.CsvImportResult;
import com.tekup.quiz.model.Question;
import com.tekup.quiz.model.Role;
import com.tekup.quiz.model.User;
import com.tekup.quiz.service.CsvQuestionService;
import com.tekup.quiz.ui.AppRoute;
import com.tekup.quiz.ui.AppScreen;
import com.tekup.quiz.ui.ScreenManager;
import com.tekup.quiz.ui.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class AdminCsvScreen implements AppScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminCsvScreen.class);

    private final ScreenManager screenManager;
    private final SessionContext sessionContext;
    private final CsvQuestionService csvQuestionService;
    private final QuestionDao questionDao;

    private final VBox root;
    private final Label subtitleLabel;
    private final Label feedbackLabel;
    private final ListView<String> reportListView;

    public AdminCsvScreen(ScreenManager screenManager,
                          SessionContext sessionContext,
                          CsvQuestionService csvQuestionService,
                          QuestionDao questionDao) {
        this.screenManager = Objects.requireNonNull(screenManager, "screenManager must not be null");
        this.sessionContext = Objects.requireNonNull(sessionContext, "sessionContext must not be null");
        this.csvQuestionService = Objects.requireNonNull(csvQuestionService, "csvQuestionService must not be null");
        this.questionDao = Objects.requireNonNull(questionDao, "questionDao must not be null");

        Label title = new Label("CSV Tools");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        subtitleLabel = new Label();
        subtitleLabel.setStyle("-fx-text-fill: #2f4050;");

        Button importButton = new Button("Import questions CSV");
        importButton.setOnAction(event -> importCsv());

        Button exportButton = new Button("Export questions CSV");
        exportButton.setOnAction(event -> exportCsv());

        Button dashboardButton = new Button("Back to dashboard");
        dashboardButton.setOnAction(event -> screenManager.show(AppRoute.ADMIN_DASHBOARD));

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> {
            sessionContext.clear();
            screenManager.show(AppRoute.AUTH);
        });

        HBox actions = new HBox(10, importButton, exportButton, dashboardButton, logoutButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        reportListView = new ListView<>();
        reportListView.setPrefHeight(360);

        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setStyle("-fx-text-fill: #b00020;");

        root = new VBox(12, title, subtitleLabel, actions, reportListView, feedbackLabel);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f7f0f6;");
    }

    @Override
    public String title() {
        return "Interactive Quiz - Admin CSV";
    }

    @Override
    public Parent root() {
        return root;
    }

    @Override
    public void onShow() {
        User currentUser = sessionContext.getCurrentUser();
        if (currentUser == null) {
            LOGGER.warn("Admin CSV access denied: no authenticated user");
            screenManager.show(AppRoute.AUTH);
            return;
        }
        if (currentUser.getRole() != Role.ADMIN) {
            LOGGER.warn("Admin CSV access denied: userId={} role={}", currentUser.getId(), currentUser.getRole());
            screenManager.show(AppRoute.CATEGORY);
            return;
        }

        subtitleLabel.setText("Import/export question datasets");
        feedbackLabel.setText("");
        reportListView.setItems(FXCollections.observableArrayList(List.of()));
    }

    private void importCsv() {
        User actor = sessionContext.getCurrentUser();
        String actorName = actor == null ? "unknown" : actor.getUsername();
        long actorId = actor == null ? -1 : actor.getId();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Questions CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file", "*.csv"));

        File selectedFile = chooser.showOpenDialog(root.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        LOGGER.info(
                "Admin CSV import started: actorId={} actor={} path={}",
                actorId,
                actorName,
                selectedFile.getAbsolutePath()
        );

        try {
            CsvImportResult result = csvQuestionService.importFromCsvWithReport(selectedFile.toPath());
            LOGGER.info(
                    "Admin CSV import completed: actorId={} actor={} path={} importedRows={} errors={}",
                    actorId,
                    actorName,
                    selectedFile.getAbsolutePath(),
                    result.getImportedCount(),
                    result.getErrors().size()
            );

            feedbackLabel.setStyle(result.hasErrors() ? "-fx-text-fill: #7a6200;" : "-fx-text-fill: #0a7d2f;");
            feedbackLabel.setText("Imported rows: " + result.getImportedCount() + " | Errors: " + result.getErrors().size());

            if (result.getErrors().isEmpty()) {
                reportListView.setItems(FXCollections.observableArrayList(List.of("No validation errors.")));
            } else {
                reportListView.setItems(FXCollections.observableArrayList(result.getErrors()));
            }
        } catch (IOException exception) {
            LOGGER.error(
                    "Admin CSV import failed (io): actorId={} actor={} path={}",
                    actorId,
                    actorName,
                    selectedFile.getAbsolutePath(),
                    exception
            );
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Could not read CSV file. Check file path and permissions.");
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Admin CSV import failed (runtime): actorId={} actor={} path={}",
                    actorId,
                    actorName,
                    selectedFile.getAbsolutePath(),
                    exception
            );
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("CSV import failed: " + exception.getMessage());
        }
    }

    private void exportCsv() {
        User actor = sessionContext.getCurrentUser();
        String actorName = actor == null ? "unknown" : actor.getUsername();
        long actorId = actor == null ? -1 : actor.getId();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Questions CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file", "*.csv"));

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        chooser.setInitialFileName("questions-export-" + timestamp + ".csv");

        File selectedFile = chooser.showSaveDialog(root.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        LOGGER.info(
                "Admin CSV export started: actorId={} actor={} path={}",
                actorId,
                actorName,
                selectedFile.getAbsolutePath()
        );

        try {
            List<Question> questions = questionDao.findAll();
            csvQuestionService.exportToCsv(selectedFile.toPath(), questions);
            LOGGER.info(
                    "Admin CSV export completed: actorId={} actor={} path={} rowCount={}",
                    actorId,
                    actorName,
                    selectedFile.getAbsolutePath(),
                    questions.size()
            );

            feedbackLabel.setStyle("-fx-text-fill: #0a7d2f;");
            feedbackLabel.setText("Exported " + questions.size() + " question(s) to " + selectedFile.getAbsolutePath());
            reportListView.setItems(FXCollections.observableArrayList(List.of("Export completed.")));
        } catch (IOException exception) {
            LOGGER.error(
                    "Admin CSV export failed (io): actorId={} actor={} path={}",
                    actorId,
                    actorName,
                    selectedFile.getAbsolutePath(),
                    exception
            );
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("Could not write CSV file. Check destination permissions.");
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Admin CSV export failed (runtime): actorId={} actor={} path={}",
                    actorId,
                    actorName,
                    selectedFile.getAbsolutePath(),
                    exception
            );
            feedbackLabel.setStyle("-fx-text-fill: #b00020;");
            feedbackLabel.setText("CSV export failed. Please try again.");
        }
    }
}
