package com.tekup.quiz.dao.jdbc;

import com.tekup.quiz.dao.CategoryDao;
import com.tekup.quiz.model.Category;
import com.tekup.quiz.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class JdbcCategoryDao implements CategoryDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcCategoryDao.class);

    @Override
    public List<Category> findAll() {
        String sql = "SELECT id, name FROM categories ORDER BY name ASC";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Category> categories = new ArrayList<>();
            while (resultSet.next()) {
                categories.add(mapCategory(resultSet));
            }
            return categories;
        } catch (SQLException exception) {
            LOGGER.error("Category query failed: operation=findAll", exception);
            throw new IllegalStateException("Failed to load categories", exception);
        }
    }

    @Override
    public Optional<Category> findById(long id) {
        String sql = "SELECT id, name FROM categories WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapCategory(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            LOGGER.error("Category query failed: operation=findById categoryId={}", id, exception);
            throw new IllegalStateException("Failed to find category by id", exception);
        }
    }

    @Override
    public Optional<Category> findByName(String name) {
        String sql = "SELECT id, name FROM categories WHERE name = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapCategory(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            LOGGER.error("Category query failed: operation=findByName categoryName={}", name, exception);
            throw new IllegalStateException("Failed to find category by name", exception);
        }
    }

    @Override
    public Category save(Category category) {
        if (category.getId() > 0) {
            return update(category);
        }
        return insert(category);
    }

    @Override
    public boolean deleteById(long id) {
        String sql = "DELETE FROM categories WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            LOGGER.error("Category delete failed: categoryId={}", id, exception);
            throw mapDeleteException(exception);
        }
    }

    @Override
    public boolean isInUse(long id) {
        String sql = "SELECT COUNT(*) FROM questions WHERE category_id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1) > 0;
                }
                return false;
            }
        } catch (SQLException exception) {
            LOGGER.error("Category usage check failed: categoryId={}", id, exception);
            throw new IllegalStateException("Failed to check category usage", exception);
        }
    }

    private Category insert(Category category) {
        String sql = "INSERT INTO categories(name) VALUES (?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, category.getName());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    category.setId(keys.getLong(1));
                }
            }
            return category;
        } catch (SQLException exception) {
            LOGGER.error("Category insert failed: categoryName={}", category.getName(), exception);
            throw mapSaveException(exception);
        }
    }

    private Category update(Category category) {
        String sql = "UPDATE categories SET name = ? WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.getName());
            statement.setLong(2, category.getId());
            statement.executeUpdate();
            return category;
        } catch (SQLException exception) {
            LOGGER.error("Category update failed: categoryId={} categoryName={}", category.getId(), category.getName(), exception);
            throw mapSaveException(exception);
        }
    }

    private Category mapCategory(ResultSet resultSet) throws SQLException {
        long id = resultSet.getLong("id");
        String name = resultSet.getString("name");
        return new Category(id, name);
    }

    private RuntimeException mapSaveException(SQLException exception) {
        if (isDuplicateKey(exception)) {
            return new IllegalArgumentException("Category name already exists");
        }
        return new IllegalStateException("Failed to save category", exception);
    }

    private RuntimeException mapDeleteException(SQLException exception) {
        if (isForeignKeyViolation(exception)) {
            return new IllegalStateException("Cannot delete category because it has linked questions");
        }
        return new IllegalStateException("Failed to delete category", exception);
    }

    private boolean isDuplicateKey(SQLException exception) {
        String sqlState = exception.getSQLState();
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase(Locale.ROOT);
        return "23505".equals(sqlState)
                || exception.getErrorCode() == 1062
                || message.contains("duplicate")
                || message.contains("unique");
    }

    private boolean isForeignKeyViolation(SQLException exception) {
        String sqlState = exception.getSQLState();
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase(Locale.ROOT);
        return "23503".equals(sqlState)
                || exception.getErrorCode() == 1451
                || message.contains("foreign key")
                || message.contains("referential");
    }
}
