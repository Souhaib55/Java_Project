package com.tekup.quiz.dao.jdbc;

import com.tekup.quiz.model.Role;
import com.tekup.quiz.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcUserDaoIntegrationTest {

    private JdbcUserDao userDao;

    @BeforeEach
    void setUp() {
        IntegrationTestDatabase.resetSchema();
        userDao = new JdbcUserDao();
    }

    @Test
    void saveShouldInsertAndFindByUsername() {
        User created = userDao.save(new User("asma", "hash1", Role.PLAYER));

        assertTrue(created.getId() > 0);
        assertTrue(userDao.findByUsername("asma").isPresent());
        assertEquals(Role.PLAYER, userDao.findByUsername("asma").orElseThrow().getRole());
    }

    @Test
    void saveShouldUpdateExistingUser() {
        User created = userDao.save(new User("asma", "hash1", Role.PLAYER));
        created.setRole(Role.ADMIN);
        created.setUsername("asma_admin");

        User updated = userDao.save(created);

        assertEquals(created.getId(), updated.getId());
        User loaded = userDao.findById(created.getId()).orElseThrow();
        assertEquals("asma_admin", loaded.getUsername());
        assertEquals(Role.ADMIN, loaded.getRole());
    }

    @Test
    void saveShouldRejectDuplicateUsername() {
        userDao.save(new User("asma", "hash1", Role.PLAYER));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDao.save(new User("asma", "hash2", Role.ADMIN))
        );

        assertNotNull(exception.getMessage());
        assertEquals("Username already exists", exception.getMessage());
    }
}
