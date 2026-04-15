package com.tekup.quiz.service;

import com.tekup.quiz.model.QuizResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ResultExportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultExportService.class);

    public void exportAsText(Path outputPath, String username, QuizResult result) throws IOException {
        Objects.requireNonNull(outputPath, "outputPath must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(result, "result must not be null");

        LOGGER.info(
                "Result export requested: user={} outputPath={} score={} totalQuestions={}",
                username,
                outputPath.toAbsolutePath(),
                result.getScore(),
                result.getTotalQuestions()
        );

        List<String> lines = new ArrayList<>();
        lines.add("Interactive Quiz Result");
        lines.add("Generated at: " + LocalDateTime.now());
        lines.add("Player: " + username);
        lines.add("Category ID: " + result.getCategoryId());
        lines.add("Score: " + result.getScore());
        lines.add("Total questions: " + result.getTotalQuestions());
        lines.add("Max possible weighted score: " + result.getQuestions().stream().mapToInt(q -> q.getDifficulty().weight()).sum());

        try {
            Files.write(outputPath, lines);
            LOGGER.info("Result export completed: user={} outputPath={}", username, outputPath.toAbsolutePath());
        } catch (IOException exception) {
            LOGGER.error("Result export failed: user={} outputPath={}", username, outputPath.toAbsolutePath(), exception);
            throw exception;
        }
    }
}
