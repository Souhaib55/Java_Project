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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

        Label csvIcon = new Label("📥");
        csvIcon.setStyle("-fx-font-size: 24px;");
        Label title = new Label("CSV Tools");
        title.getStyleClass().add("screen-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button dashboardButton = new Button("← Dashboard");
        dashboardButton.getStyleClass().add("nav-button");
        dashboardButton.setOnAction(event -> screenManager.show(AppRoute.ADMIN_DASHBOARD));

        Button logoutButton = new Button("Logout");
        logoutButton.getStyleClass().add("danger-button");
        logoutButton.setOnAction(event -> {
            sessionContext.clear();
            screenManager.show(AppRoute.AUTH);
        });

        HBox titleRow = new HBox(12, csvIcon, title, spacer, dashboardButton, logoutButton);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        subtitleLabel = new Label();
        subtitleLabel.getStyleClass().add("screen-subtitle");

        // Action card
        Label actHeader = new Label("BULK OPERATIONS");
        actHeader.setStyle("-fx-text-fill: #4a5a78; -fx-font-size: 11px; -fx-font-weight: 700;");

        Button importButton = new Button("📂  Import Questions from CSV");
        importButton.getStyleClass().add("primary-button");
        importButton.setMaxWidth(Double.MAX_VALUE);
        importButton.setOnAction(event -> importCsv());

        Button exportButton = new Button("💾  Export All Questions to CSV");
        exportButton.getStyleClass().add("primary-button");
        exportButton.setMaxWidth(Double.MAX_VALUE);
        exportButton.setOnAction(event -> exportCsv());

        Label csvHint = new Label("CSV format: type, category_id, difficulty, prompt, option_a, option_b, option_c, option_d, correct_answer");
        csvHint.setStyle("-fx-text-fill: #4a5a78; -fx-font-size: 11px;");
        csvHint.setWrapText(true);

        VBox actionCard = new VBox(10, actHeader, importButton, exportButton, csvHint);
        actionCard.getStyleClass().add("stat-card");

        // Report list
        Label reportHeader = new Label("IMPORT / EXPORT LOG");
        reportHeader.setStyle("-fx-text-fill: #4a5a78; -fx-font-size: 11px; -fx-font-weight: 700;");

        reportListView = new ListView<>();
        reportListView.getStyleClass().add("modern-list");
        VBox.setVgrow(reportListView, Priority.ALWAYS);

        VBox reportSection = new VBox(8, reportHeader, reportListView);
        VBox.setVgrow(reportSection, Priority.ALWAYS);

        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.getStyleClass().add("feedback-error");

        root = new VBox(16, titleRow, subtitleLabel, actionCard, reportSection, feedbackLabel);
        root.getStyleClass().addAll("app-root", "screen-admin");
        root.setPadding(new Insets(28));
        VBox.setVgrow(reportSection, Priority.ALWAYS);
    }

    @Override
    public String title() {
        return "QuizMaster — CSV Tools";
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

        subtitleLabel.setText("Bulk import or export questions via CSV file");
        setFeedback("", "feedback-error");
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

            setFeedback(
                    "Imported rows: " + result.getImportedCount() + " | Errors: " + result.getErrors().size(),
                    result.hasErrors() ? "feedback-warning" : "feedback-success"
            );

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
            setFeedback("Could not read CSV file. Check file path and permissions.", "feedback-error");
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Admin CSV import failed (runtime): actorId={} actor={} path={}",
                    actorId,
                    actorName,
                    selectedFile.getAbsolutePath(),
                    exception
            );
            setFeedback("CSV import failed: " + exception.getMessage(), "feedback-error");
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

            setFeedback(
                    "Exported " + questions.size() + " question(s) to " + selectedFile.getAbsolutePath(),
                    "feedback-success"
            );
            reportListView.setItems(FXCollections.observableArrayList(List.of("Export completed.")));
        } catch (IOException exception) {
            LOGGER.error(
                    "Admin CSV export failed (io): actorId={} actor={} path={}",
                    actorId,
                    actorName,
                    selectedFile.getAbsolutePath(),
                    exception
            );
            setFeedback("Could not write CSV file. Check destination permissions.", "feedback-error");
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Admin CSV export failed (runtime): actorId={} actor={} path={}",
                    actorId,
                    actorName,
                    selectedFile.getAbsolutePath(),
                    exception
            );
            setFeedback("CSV export failed. Please try again.", "feedback-error");
        }
    }

    private void setFeedback(String message, String styleClass) {
        feedbackLabel.getStyleClass().removeAll("feedback-error", "feedback-success", "feedback-warning");
        feedbackLabel.getStyleClass().add(styleClass);
        feedbackLabel.setText(message);
    }
}
