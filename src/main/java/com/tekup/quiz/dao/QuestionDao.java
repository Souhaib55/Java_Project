package com.tekup.quiz.dao;

import com.tekup.quiz.model.Difficulty;
import com.tekup.quiz.model.Question;

import java.util.List;
import java.util.Optional;

public interface QuestionDao {
    Optional<Question> findById(long id);

    List<Question> findAll();

    List<Question> findByCategory(long categoryId);

    List<Question> findByCategoryAndDifficulty(long categoryId, Difficulty difficulty, int limit);

    Question save(Question question);

    boolean deleteById(long id);
}
