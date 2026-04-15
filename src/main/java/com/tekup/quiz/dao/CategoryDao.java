package com.tekup.quiz.dao;

import com.tekup.quiz.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryDao {
    List<Category> findAll();

    Optional<Category> findById(long id);

    Optional<Category> findByName(String name);

    Category save(Category category);

    boolean deleteById(long id);

    boolean isInUse(long id);
}
