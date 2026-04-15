package com.tekup.quiz.service;

import com.tekup.quiz.dao.QuestionDao;
import com.tekup.quiz.model.Difficulty;
import com.tekup.quiz.model.CsvImportResult;
import com.tekup.quiz.model.McqQuestion;
import com.tekup.quiz.model.Question;
import com.tekup.quiz.model.QuestionType;
import com.tekup.quiz.model.TrueFalseQuestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongPredicate;

public class CsvQuestionService {
    private static final String HEADER = "type,categoryId,difficulty,prompt,optionA,optionB,optionC,optionD,correctAnswer";
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvQuestionService.class);

    private final QuestionDao questionDao;
    private final LongPredicate categoryExistsValidator;

    public CsvQuestionService(QuestionDao questionDao) {
        this(questionDao, categoryId -> true);
    }

    public CsvQuestionService(QuestionDao questionDao, LongPredicate categoryExistsValidator) {
        this.questionDao = Objects.requireNonNull(questionDao, "questionDao must not be null");
        this.categoryExistsValidator = Objects.requireNonNull(categoryExistsValidator, "categoryExistsValidator must not be null");
    }

    public List<Question> importFromCsv(Path csvPath) throws IOException {
        LOGGER.info("CSV import started: path={}", csvPath.toAbsolutePath());
        List<String> lines = Files.readAllLines(csvPath);
        if (lines.isEmpty()) {
            LOGGER.info("CSV import completed: path={} importedCount=0", csvPath.toAbsolutePath());
            return List.of();
        }

        int startIndex = resolveStartIndex(lines);
        List<Question> savedQuestions = new ArrayList<>();
        for (int index = startIndex; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.isEmpty()) {
                continue;
            }
            Question question = parseLine(line, index + 1);
            validateCategoryReference(question.getCategoryId());
            savedQuestions.add(questionDao.save(question));
        }

        LOGGER.info("CSV import completed: path={} importedCount={}", csvPath.toAbsolutePath(), savedQuestions.size());

        return savedQuestions;
    }

    public CsvImportResult importFromCsvWithReport(Path csvPath) throws IOException {
        LOGGER.info("CSV import (with report) started: path={}", csvPath.toAbsolutePath());
        List<String> lines = Files.readAllLines(csvPath);
        if (lines.isEmpty()) {
            LOGGER.info("CSV import (with report) completed: path={} importedCount=0 errors=0", csvPath.toAbsolutePath());
            return new CsvImportResult(0, List.of());
        }

        int startIndex = resolveStartIndex(lines);
        int importedCount = 0;
        List<String> errors = new ArrayList<>();

        for (int index = startIndex; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.isEmpty()) {
                continue;
            }

            try {
                Question question = parseLine(line, index + 1);
                validateCategoryReference(question.getCategoryId());
                questionDao.save(question);
                importedCount++;
            } catch (RuntimeException exception) {
                errors.add("Line " + (index + 1) + ": " + exception.getMessage());
                LOGGER.warn(
                        "CSV import row rejected: path={} line={} reason={}",
                        csvPath.toAbsolutePath(),
                        index + 1,
                        exception.getMessage()
                );
            }
        }

        LOGGER.info(
                "CSV import (with report) completed: path={} importedCount={} errors={}",
                csvPath.toAbsolutePath(),
                importedCount,
                errors.size()
        );

        return new CsvImportResult(importedCount, errors);
    }

    public void exportToCsv(Path csvPath, List<Question> questions) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);

        for (Question question : questions) {
            String[] options = extractOptions(question);
            String row = String.join(",",
                    question.getType().name(),
                    String.valueOf(question.getCategoryId()),
                    question.getDifficulty().name(),
                    sanitize(question.getPrompt()),
                    sanitize(options[0]),
                    sanitize(options[1]),
                    sanitize(options[2]),
                    sanitize(options[3]),
                    question.getCorrectAnswer()
            );
            lines.add(row);
        }

        Files.write(csvPath, lines);
        LOGGER.info("CSV export completed: path={} rowCount={}", csvPath.toAbsolutePath(), questions.size());
    }

    private Question parseLine(String line, int rowNumber) {
        List<String> parts = splitCsvColumns(line, rowNumber);
        if (parts.size() != 9) {
            throw new IllegalArgumentException("Invalid CSV row at line " + rowNumber + ": expected 9 columns");
        }

        QuestionType type = QuestionType.valueOf(parts.get(0).trim().toUpperCase());
        long categoryId = Long.parseLong(parts.get(1).trim());
        Difficulty difficulty = Difficulty.valueOf(parts.get(2).trim().toUpperCase());
        String prompt = parts.get(3).trim();
        String optionA = parts.get(4).trim();
        String optionB = parts.get(5).trim();
        String optionC = parts.get(6).trim();
        String optionD = parts.get(7).trim();
        String correctAnswer = parts.get(8).trim();

        if (type == QuestionType.MCQ) {
            return new McqQuestion(categoryId, difficulty, prompt, optionA, optionB, optionC, optionD, correctAnswer);
        }

        return new TrueFalseQuestion(categoryId, difficulty, prompt, correctAnswer);
    }

    private List<String> splitCsvColumns(String line, int rowNumber) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);

            if (currentChar == '"') {
                if (insideQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    insideQuotes = !insideQuotes;
                }
                continue;
            }

            if (currentChar == ',' && !insideQuotes) {
                columns.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(currentChar);
        }

        if (insideQuotes) {
            throw new IllegalArgumentException("Invalid CSV row at line " + rowNumber + ": unmatched quote");
        }

        columns.add(current.toString());
        return columns;
    }

    private void validateCategoryReference(long categoryId) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("Invalid category id");
        }
        if (!categoryExistsValidator.test(categoryId)) {
            throw new IllegalArgumentException("Selected category does not exist");
        }
    }

    private int resolveStartIndex(List<String> lines) {
        if (lines.isEmpty()) {
            return 0;
        }
        return lines.get(0).trim().equalsIgnoreCase(HEADER) ? 1 : 0;
    }

    private String[] extractOptions(Question question) {
        if (question instanceof McqQuestion mcq) {
            return new String[] {
                    mcq.getOptionA(),
                    mcq.getOptionB(),
                    mcq.getOptionC(),
                    mcq.getOptionD()
            };
        }

        return new String[] {"TRUE", "FALSE", "", ""};
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace(',', ';').trim();
    }
}
