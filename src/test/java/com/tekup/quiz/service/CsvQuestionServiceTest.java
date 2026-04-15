package com.tekup.quiz.service;

import com.tekup.quiz.dao.QuestionDao;
import com.tekup.quiz.model.CsvImportResult;
import com.tekup.quiz.model.Difficulty;
import com.tekup.quiz.model.McqQuestion;
import com.tekup.quiz.model.Question;
import com.tekup.quiz.model.QuestionType;
import com.tekup.quiz.model.TrueFalseQuestion;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvQuestionServiceTest {

    @Test
    void importFromCsvWithReportShouldImportValidRowsAndCollectErrors() throws IOException {
        RecordingQuestionDao questionDao = new RecordingQuestionDao();
        CsvQuestionService csvQuestionService = new CsvQuestionService(questionDao);

        Path csvPath = Files.createTempFile("quiz-import", ".csv");
        Files.writeString(csvPath, String.join("\n",
                "type,categoryId,difficulty,prompt,optionA,optionB,optionC,optionD,correctAnswer",
                "MCQ,1,EASY,What is Java?,A language,An animal,A city,A car,A",
                "TRUE_FALSE,1,MEDIUM,The sky is blue.,TRUE,FALSE,,,TRUE",
                "BADTYPE,1,EASY,Invalid type,A,B,C,D,A",
                "MCQ,1,UNKNOWN,Invalid difficulty,A,B,C,D,A"
        ));

        CsvImportResult result = csvQuestionService.importFromCsvWithReport(csvPath);

        assertEquals(2, result.getImportedCount());
        assertEquals(2, result.getErrors().size());
        assertEquals(2, questionDao.savedQuestions.size());
        assertTrue(result.getErrors().get(0).contains("Line 4"));
        assertTrue(result.getErrors().get(1).contains("Line 5"));
    }

    @Test
    void exportToCsvShouldWriteRowsWithExpectedHeader() throws IOException {
        RecordingQuestionDao questionDao = new RecordingQuestionDao();
        CsvQuestionService csvQuestionService = new CsvQuestionService(questionDao);

        List<Question> questions = List.of(
                new McqQuestion(1L, Difficulty.EASY, "Q1", "A1", "B1", "C1", "D1", "A"),
                new TrueFalseQuestion(1L, Difficulty.HARD, "Q2", "TRUE")
        );

        Path outputPath = Files.createTempFile("quiz-export", ".csv");
        csvQuestionService.exportToCsv(outputPath, questions);

        List<String> lines = Files.readAllLines(outputPath);
        assertEquals(3, lines.size());
        assertEquals("type,categoryId,difficulty,prompt,optionA,optionB,optionC,optionD,correctAnswer", lines.get(0));
        assertTrue(lines.get(1).startsWith("MCQ,1,EASY,Q1"));
        assertTrue(lines.get(2).startsWith("TRUE_FALSE,1,HARD,Q2"));
    }

    @Test
    void importFromCsvWithReportShouldParseQuotedCommas() throws IOException {
        RecordingQuestionDao questionDao = new RecordingQuestionDao();
        CsvQuestionService csvQuestionService = new CsvQuestionService(questionDao);

        Path csvPath = Files.createTempFile("quiz-import-quoted", ".csv");
        Files.writeString(csvPath, String.join("\n",
                "type,categoryId,difficulty,prompt,optionA,optionB,optionC,optionD,correctAnswer",
                "MCQ,1,EASY,\"What, exactly, is Java?\",\"A, language\",Tool,City,Car,A"
        ));

        CsvImportResult result = csvQuestionService.importFromCsvWithReport(csvPath);

        assertEquals(1, result.getImportedCount());
        assertTrue(result.getErrors().isEmpty());
        assertEquals(1, questionDao.savedQuestions.size());

        Question saved = questionDao.savedQuestions.get(0);
        assertEquals(QuestionType.MCQ, saved.getType());
        assertInstanceOf(McqQuestion.class, saved);
        assertEquals("What, exactly, is Java?", saved.getPrompt());
        assertEquals("A, language", saved.getOptions().get(0));
    }

    @Test
    void importFromCsvWithReportShouldRejectUnknownCategoryFromValidator() throws IOException {
        RecordingQuestionDao questionDao = new RecordingQuestionDao();
        CsvQuestionService csvQuestionService = new CsvQuestionService(questionDao, categoryId -> categoryId == 1L);

        Path csvPath = Files.createTempFile("quiz-import-categories", ".csv");
        Files.writeString(csvPath, String.join("\n",
                "type,categoryId,difficulty,prompt,optionA,optionB,optionC,optionD,correctAnswer",
                "MCQ,1,EASY,Valid row,A,B,C,D,A",
                "MCQ,999,EASY,Invalid category,A,B,C,D,A"
        ));

        CsvImportResult result = csvQuestionService.importFromCsvWithReport(csvPath);

        assertEquals(1, result.getImportedCount());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Line 3"));
        assertTrue(result.getErrors().get(0).contains("Selected category does not exist"));
    }

    private static class RecordingQuestionDao implements QuestionDao {
        private long sequence = 0;
        private final List<Question> savedQuestions = new ArrayList<>();

        @Override
        public Optional<Question> findById(long id) {
            return savedQuestions.stream().filter(q -> q.getId() == id).findFirst();
        }

        @Override
        public List<Question> findAll() {
            return List.copyOf(savedQuestions);
        }

        @Override
        public List<Question> findByCategory(long categoryId) {
            return savedQuestions.stream().filter(q -> q.getCategoryId() == categoryId).toList();
        }

        @Override
        public List<Question> findByCategoryAndDifficulty(long categoryId, Difficulty difficulty, int limit) {
            return savedQuestions.stream()
                    .filter(q -> q.getCategoryId() == categoryId && q.getDifficulty() == difficulty)
                    .limit(limit)
                    .toList();
        }

        @Override
        public Question save(Question question) {
            if (question.getId() <= 0) {
                sequence++;
                question.setId(sequence);
            }
            savedQuestions.add(question);
            return question;
        }

        @Override
        public boolean deleteById(long id) {
            return savedQuestions.removeIf(q -> q.getId() == id);
        }
    }
}
