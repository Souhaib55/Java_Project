package com.tekup.quiz.dao;

import com.tekup.quiz.model.AttemptView;
import com.tekup.quiz.model.QuizAttempt;

import java.util.List;

public interface QuizAttemptDao {
    QuizAttempt save(QuizAttempt attempt);

    List<QuizAttempt> findByUser(long userId);

    List<QuizAttempt> findTopGlobal(int limit);

    List<QuizAttempt> findTopByCategory(long categoryId, int limit);

    List<AttemptView> findTopGlobalView(int limit);

    List<AttemptView> findTopByCategoryView(long categoryId, int limit);

    List<AttemptView> findRecentByUserView(long userId, int limit);
}
