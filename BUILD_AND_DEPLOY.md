# Build and Deploy Runbook

## Build prerequisites
- Java 17+ JDK (not JRE)
- Maven Wrapper files (included in repository)
- MySQL 8+ for runtime

## Database initialization
1. Create schema and tables using:
   - src/main/resources/db/schema.sql
2. Update runtime DB config in:
   - src/main/resources/application.properties
3. Optionally tune DB pool settings in runtime config:
   - db.pool.maxSize, db.pool.minIdle, db.pool.connectionTimeoutMs,
     db.pool.validationTimeoutMs, db.pool.idleTimeoutMs, db.pool.maxLifetimeMs
4. Configure session timeout policy:
   - session.timeoutMinutes

## Build and test
- Run tests (Windows):
   - mvnw.cmd test
- Run tests (Linux/macOS):
   - ./mvnw test
- Build artifact (Windows):
   - mvnw.cmd package
- Build artifact (Linux/macOS):
   - ./mvnw package

## Run JavaFX app
- Windows:
   - mvnw.cmd javafx:run
- Linux/macOS:
   - ./mvnw javafx:run

## If Maven is unavailable
Use IDE run configuration:
1. Ensure dependencies are resolved by IDE.
2. Run main class:
   - com.tekup.quiz.MainApp
3. Confirm MySQL is running and app credentials are valid.

## Manual smoke checklist
1. Register or login as PLAYER.
2. Run quiz and submit answers.
3. Open leaderboard and history.
4. Login as ADMIN.
5. Open admin dashboard and verify:
   - Category management works.
   - Question management works.
   - CSV import/export works.
6. Validate startup resilience:
   - Stop MySQL and launch app, then confirm startup error screen is shown.
   - Restore MySQL and launch app, then confirm app reaches authentication screen.
7. Validate quiz edge-case handling:
   - Select a category+difficulty with no questions and confirm a clear warning is shown.
8. Validate CSV row validation:
   - Import a CSV with an unknown category id and confirm row-level error reporting.
9. Validate session safety:
   - Keep a logged-in session idle beyond timeout and confirm redirect to authentication.
10. Validate operational logs:
   - Confirm auth, quiz, leaderboard/history, CSV, result export, and DB connection lifecycle events appear in logs.
   - Confirm admin action audit logs appear for category/question/CSV operations.
   - Confirm screen-level recovery failures are logged with actionable context.

## Packaging note
If distribution without Maven is required, create a runnable JAR from IDE build artifacts and include:
- application.properties
- DB setup instructions
