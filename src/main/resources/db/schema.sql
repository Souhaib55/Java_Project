CREATE DATABASE IF NOT EXISTS quiz_app;
USE quiz_app;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'PLAYER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    category_id BIGINT NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    prompt TEXT NOT NULL,
    option_a VARCHAR(255),
    option_b VARCHAR(255),
    option_c VARCHAR(255),
    option_d VARCHAR(255),
    correct_answer VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_questions_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS quiz_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT,
    score INT NOT NULL,
    total_questions INT NOT NULL,
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attempts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_attempts_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- Safe index creation for MySQL versions that do not support CREATE INDEX IF NOT EXISTS.
SET @idx := (
        SELECT COUNT(*)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
            AND table_name = 'quiz_attempts'
            AND index_name = 'idx_attempts_score'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_attempts_score ON quiz_attempts(score DESC)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx := (
        SELECT COUNT(*)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
            AND table_name = 'quiz_attempts'
            AND index_name = 'idx_attempts_user'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_attempts_user ON quiz_attempts(user_id)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx := (
        SELECT COUNT(*)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
            AND table_name = 'quiz_attempts'
            AND index_name = 'idx_attempts_category_score'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_attempts_category_score ON quiz_attempts(category_id, score DESC)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx := (
        SELECT COUNT(*)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
            AND table_name = 'questions'
            AND index_name = 'idx_questions_category_difficulty'
);
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_questions_category_difficulty ON questions(category_id, difficulty)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO categories(name) VALUES
    ('Science'),
    ('History'),
    ('Programming');

SET @cat_science := (SELECT id FROM categories WHERE name = 'Science' LIMIT 1);
SET @cat_history := (SELECT id FROM categories WHERE name = 'History' LIMIT 1);
SET @cat_programming := (SELECT id FROM categories WHERE name = 'Programming' LIMIT 1);

-- Seed up to 30 questions per category (ensures enough questions for quiz length selection).
DROP TEMPORARY TABLE IF EXISTS tmp_numbers;
CREATE TEMPORARY TABLE tmp_numbers (n INT PRIMARY KEY);
INSERT INTO tmp_numbers (n) VALUES
    (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),
    (11),(12),(13),(14),(15),(16),(17),(18),(19),(20),
    (21),(22),(23),(24),(25),(26),(27),(28),(29),(30);

SET @science_missing := 30 - (SELECT COUNT(*) FROM questions WHERE category_id = @cat_science);
SET @science_missing := IF(@science_missing < 0, 0, @science_missing);
INSERT INTO questions(type, category_id, difficulty, prompt, option_a, option_b, option_c, option_d, correct_answer)
SELECT 'MCQ', @cat_science,
       CASE MOD(n, 3) WHEN 1 THEN 'EASY' WHEN 2 THEN 'MEDIUM' ELSE 'HARD' END,
       CONCAT('Science practice question ', n),
       'Option A', 'Option B', 'Option C', 'Option D', 'A'
FROM tmp_numbers
WHERE n <= @science_missing;

SET @history_missing := 30 - (SELECT COUNT(*) FROM questions WHERE category_id = @cat_history);
SET @history_missing := IF(@history_missing < 0, 0, @history_missing);
INSERT INTO questions(type, category_id, difficulty, prompt, option_a, option_b, option_c, option_d, correct_answer)
SELECT 'MCQ', @cat_history,
       CASE MOD(n, 3) WHEN 1 THEN 'EASY' WHEN 2 THEN 'MEDIUM' ELSE 'HARD' END,
       CONCAT('History practice question ', n),
       'Option A', 'Option B', 'Option C', 'Option D', 'A'
FROM tmp_numbers
WHERE n <= @history_missing;

SET @programming_missing := 30 - (SELECT COUNT(*) FROM questions WHERE category_id = @cat_programming);
SET @programming_missing := IF(@programming_missing < 0, 0, @programming_missing);
INSERT INTO questions(type, category_id, difficulty, prompt, option_a, option_b, option_c, option_d, correct_answer)
SELECT 'MCQ', @cat_programming,
       CASE MOD(n, 3) WHEN 1 THEN 'EASY' WHEN 2 THEN 'MEDIUM' ELSE 'HARD' END,
       CONCAT('Programming practice question ', n),
       'Option A', 'Option B', 'Option C', 'Option D', 'A'
FROM tmp_numbers
WHERE n <= @programming_missing;

DROP TEMPORARY TABLE IF EXISTS tmp_numbers;
