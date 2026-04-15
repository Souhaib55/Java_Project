package com.tekup.quiz.model;

import java.util.List;

public class McqQuestion extends Question {
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    public McqQuestion(long id,
                       long categoryId,
                       Difficulty difficulty,
                       String prompt,
                       String optionA,
                       String optionB,
                       String optionC,
                       String optionD,
                       String correctAnswer) {
        super(id, QuestionType.MCQ, categoryId, difficulty, prompt, correctAnswer);
        this.optionA = validateOption(optionA, "A");
        this.optionB = validateOption(optionB, "B");
        this.optionC = validateOption(optionC, "C");
        this.optionD = validateOption(optionD, "D");
    }

    public McqQuestion(long categoryId,
                       Difficulty difficulty,
                       String prompt,
                       String optionA,
                       String optionB,
                       String optionC,
                       String optionD,
                       String correctAnswer) {
        this(0L, categoryId, difficulty, prompt, optionA, optionB, optionC, optionD, correctAnswer);
    }

    public String getOptionA() {
        return optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    @Override
    public List<String> getOptions() {
        return List.of(optionA, optionB, optionC, optionD);
    }

    private String validateOption(String value, String optionLabel) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Option " + optionLabel + " must not be blank");
        }
        return value.trim();
    }
}
