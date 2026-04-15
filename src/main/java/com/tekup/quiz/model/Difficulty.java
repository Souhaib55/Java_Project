package com.tekup.quiz.model;

public enum Difficulty {
    EASY(1),
    MEDIUM(2),
    HARD(3);

    private final int weight;

    Difficulty(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
