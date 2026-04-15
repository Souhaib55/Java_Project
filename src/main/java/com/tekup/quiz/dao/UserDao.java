package com.tekup.quiz.dao;

import com.tekup.quiz.model.User;

import java.util.Optional;

public interface UserDao {
    Optional<User> findById(long id);

    Optional<User> findByUsername(String username);

    User save(User user);
}
