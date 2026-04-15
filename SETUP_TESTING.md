# Testing Setup

## Overview
This project uses two testing levels:
- Unit tests for services and edge-case logic.
- Integration tests for JDBC DAOs using an in-memory H2 database.

## Prerequisites
- Java 17+ JDK (not JRE)
- Maven Wrapper files (included in repository)

## Default test configuration
Integration tests use the test resource file:
- src/test/resources/application.properties

The H2 URL is configured so each test class can reset schema safely in-memory.
Pool tuning values are also set in the same file for deterministic test behavior.

## Run tests with Maven Wrapper
- Run full suite (Windows):
  - mvnw.cmd test
- Run full suite (Linux/macOS):
  - ./mvnw test

## CI validation
- CI workflow path:
  - .github/workflows/ci.yml
- CI verification command:
  - ./mvnw -B -ntp verify

## If Maven is unavailable
Use your IDE test runner:
1. Open the project in IntelliJ or VS Code Java support.
2. Run all tests under src/test/java.
3. Ensure src/test/resources is included in classpath.

## Important test helper
DAO integration tests rely on:
- src/test/java/com/tekup/quiz/dao/jdbc/IntegrationTestDatabase.java

This helper resets schema before each integration test and inserts seed data.

## Current test coverage focus
- Service tests:
  - AuthService, QuizService, LeaderboardService, HistoryService, CsvQuestionService
- DAO integration tests:
  - JdbcUserDao, JdbcCategoryDao, JdbcQuestionDao, JdbcQuizAttemptDao

## Edge-case checks covered
- Quiz generation when no questions are available for the selected setup.
- CSV parsing for quoted values containing commas.
- CSV row-level validation for unknown category ids.
- Session timeout expiration and one-time re-auth notice behavior.
- Admin form validation for category/question constraints.

## Troubleshooting
- If tests fail with DB connection errors:
  - Check that test application.properties exists and points to H2.
- If tests fail with table-not-found errors:
  - Verify resetSchema() is called in @BeforeEach.
- If role-based tests fail unexpectedly:
  - Verify Role enum values are PLAYER and ADMIN.
- If session timeout tests fail unexpectedly:
  - Verify session.timeoutMinutes is present in configuration and positive.
- If UI save actions reject inputs unexpectedly:
  - Verify question/category values satisfy validation rules in FormValidator.
- If Maven output shows ERROR logs but the suite still reports Failures: 0, Errors: 0:
  - This can be expected for tests that intentionally trigger duplicate-key or FK-constraint exceptions.
  - DAO logging records these expected exceptions at ERROR level before tests assert the thrown error.
- If build fails with "No compiler is provided in this environment":
  - Install JDK 17 and ensure JAVA_HOME points to that JDK.
  - Reopen terminal and verify with: java -version and javac -version.
