# Java Project Explanation (Interactive Quiz)

## 1) Purpose and Goal
This project is an **Interactive Quiz** application built with Java. It provides a JavaFX desktop UI (with a console fallback) where users can register or log in, take timed quizzes, see results, and view leaderboards/history. Admin users can manage categories and questions and import/export question sets via CSV.

**Primary goals:**
- Provide a clean end-to-end quiz experience for players.
- Enforce role-based actions (PLAYER vs ADMIN).
- Persist data in MySQL with safe validation and structured logging.
- Support CSV import/export for questions.

## 2) Key Features (What the app does)
**Player features**
- Register or log in.
- Select category, difficulty, and number of questions.
- Timed quiz with automatic timeout handling per question.
- See results with weighted scoring (difficulty weight).
- View global and category leaderboards.
- View recent quiz history.

**Admin features**
- Manage categories (create/update/delete).
- Manage questions (MCQ and True/False).
- CSV import with row-level validation report.
- CSV export of all questions.

## 3) Tech Stack and Dependencies
- Java 17
- JavaFX (UI)
- MySQL 8+ (runtime database)
- HikariCP (DB connection pool)
- BCrypt (password hashing)
- SLF4J (logging)
- JUnit 5 + H2 (tests)

All dependencies are declared in `pom.xml`.

## 4) Folder Structure (Top Level)
```
projetjava/
  src/
    main/
      java/
      resources/
    test/
      java/
      resources/
  pom.xml
  mvnw / mvnw.cmd
  README.md
  BUILD_AND_DEPLOY.md
  SETUP_TESTING.md
  DEV_PLAN.md
  target/
```

**Role of each top-level area:**
- `src/main/java`: Application code.
- `src/main/resources`: Configuration + database schema.
- `src/test/java`: Unit and integration tests.
- `src/test/resources`: Test configuration (H2 DB).
- `pom.xml`: Build configuration + dependencies.
- `mvnw` / `mvnw.cmd`: Maven wrapper (run without installing Maven globally).
- `target/`: Build outputs.

## 5) Detailed Package Responsibilities
### `com.tekup.quiz.model`
Holds domain entities and enums. These are plain objects with validation rules.
- `User`, `Role`: authentication identity + roles (PLAYER / ADMIN).
- `Category`: question grouping.
- `Question` (abstract), `McqQuestion`, `TrueFalseQuestion`: question types and validation logic.
- `Difficulty`: EASY/MEDIUM/HARD with weights (1/2/3).
- `QuestionType`: MCQ / TRUE_FALSE.
- `QuizAttempt`: persisted quiz attempt.
- `QuizResult`: in-memory result of a quiz session.
- `AttemptView`: denormalized leaderboard/history row.
- `CsvImportResult`: import summary (success count + errors).

### `com.tekup.quiz.dao`
DAO interfaces that define persistence operations (CRUD and search):
- `UserDao`, `CategoryDao`, `QuestionDao`, `QuizAttemptDao`.

### `com.tekup.quiz.dao.jdbc`
Concrete JDBC implementations. These classes:
- Use `DatabaseManager` to get pooled connections.
- Run SQL queries.
- Map `ResultSet` rows to model objects.
- Translate SQL errors into clear exceptions (duplicate key, FK violations).

### `com.tekup.quiz.service`
Business logic that coordinates models + DAOs:
- `AuthService`: register/login with BCrypt, validation, and logging.
- `QuizService`: generate quiz and evaluate answers, save attempts.
- `LeaderboardService`: top scores (global and by category).
- `HistoryService`: recent attempts for a user.
- `CsvQuestionService`: parse CSV, validate, import/export questions.
- `ResultExportService`: create a text file of quiz results.

### `com.tekup.quiz.ui`
UI infrastructure:
- `MainApp`: JavaFX entry point. Builds DAOs/services and registers screens.
- `ScreenManager`: route-based screen navigation with session checks.
- `SessionContext`: tracks current user, selected category/difficulty, session timeout.
- `AppRoute`: route identifiers.
- `AppScreen`: interface implemented by each screen.
- `ConsoleApp`: fallback console login/register flow (bootstrap).

### `com.tekup.quiz.ui.screens`
JavaFX UI screens. Examples:
- `AuthScreen`: login/register UI.
- `CategorySelectionScreen`: choose category/difficulty/question count.
- `QuizScreen`: timed quiz UI and submission flow.
- `ResultScreen`: shows score + export button.
- `LeaderboardScreen`: global or category leaderboard.
- `HistoryScreen`: recent attempts.
- Admin screens: `AdminDashboardScreen`, `AdminCategoryScreen`, `AdminQuestionScreen`, `AdminCsvScreen`.

### `com.tekup.quiz.util`
Shared utilities:
- `AppConfig`: reads `application.properties` and environment overrides.
- `DatabaseManager`: initializes HikariCP, verifies DB connectivity, manages shutdown.
- `FormValidator`: validates category/question inputs.
- `PasswordHasher`: wraps BCrypt hashing.

### `src/main/resources`
- `application.properties`: DB config + quiz settings.
- `db/schema.sql`: SQL schema for MySQL tables and initial seed data.

### `src/test`
- Unit and integration tests (H2 in-memory DB for DAO integration tests).

## 6) Data Model Overview (Conceptual)
- **User**: `id`, `username`, `passwordHash`, `role`.
- **Category**: `id`, `name`.
- **Question**: `id`, `type`, `categoryId`, `difficulty`, `prompt`, `correctAnswer`.
  - **MCQ**: also stores options A/B/C/D.
  - **True/False**: only TRUE/FALSE options.
- **QuizAttempt**: `userId`, `categoryId`, `score`, `totalQuestions`, `attemptedAt`.
- **QuizResult**: in-memory result (score + questions list).
- **AttemptView**: joined view for leaderboard/history with category name and username.

Scoring: difficulty weights are defined in `Difficulty` (EASY=1, MEDIUM=2, HARD=3). `QuizService` totals weights for correct answers.

## 7) Database Schema (MySQL)
Tables (from `src/main/resources/db/schema.sql`):
- `users`: stores username, password hash, role.
- `categories`: category names.
- `questions`: question data with FK to `categories`.
- `quiz_attempts`: results with FK to `users` and optional `categories`.

Relationships:
- `questions.category_id -> categories.id`
- `quiz_attempts.user_id -> users.id`
- `quiz_attempts.category_id -> categories.id`

Seed data: categories `Science`, `History`, `Programming` are inserted by default.

## 8) How the App Starts (Startup Flow)
1. `MainApp` launches JavaFX.
2. `DatabaseManager.verifyConnection()` checks DB connectivity.
3. JDBC DAOs are created.
4. Service objects are created using DAOs.
5. Screens are registered in `ScreenManager` with routes.
6. The first screen shown is `AuthScreen`.

If DB connection fails, `MainApp` shows a startup error screen and closes the pool.

## 9) User Flows (How folders connect together)
**Player flow**
1. `AuthScreen` -> `AuthService` -> `UserDao` (login/register).
2. `CategorySelectionScreen` loads categories via `CategoryDao` and reads `quiz.defaultQuestionCount`.
3. `QuizScreen` calls `QuizService.generateQuiz(...)` and reads `quiz.secondsPerQuestion`.
4. `QuizService.evaluate(...)` saves `QuizAttempt` via `QuizAttemptDao`.
5. `ResultScreen` shows score and can export with `ResultExportService`.
6. `LeaderboardScreen` and `HistoryScreen` use `LeaderboardService` and `HistoryService`.

**Admin flow**
1. `AdminDashboardScreen` is visible only for role ADMIN.
2. `AdminCategoryScreen` uses `CategoryDao` and `FormValidator`.
3. `AdminQuestionScreen` uses `QuestionDao`, `CategoryDao`, and `FormValidator`.
4. `AdminCsvScreen` uses `CsvQuestionService` to import/export.

**Session safety**
- `SessionContext` tracks last activity and uses `session.timeoutMinutes`.
- `ScreenManager` redirects expired sessions back to `AuthScreen`.

## 10) CSV Import/Export Details
CSV header format (expected):
```
type,categoryId,difficulty,prompt,optionA,optionB,optionC,optionD,correctAnswer
```
Validation rules (examples):
- Category ID must exist.
- MCQ correct answer must be A/B/C/D.
- True/False correct answer must be TRUE/FALSE.
- Empty rows are skipped.

Import reports list row errors in `CsvImportResult`.

## 11) Configuration Keys
From `src/main/resources/application.properties`:
- `db.url`, `db.username`, `db.password`
- `db.pool.*` (Hikari settings)
- `quiz.defaultQuestionCount`
- `quiz.secondsPerQuestion`
- `session.timeoutMinutes`

`AppConfig` also supports environment overrides (for example, `DB_URL`).

## 12) How to Run the Project (Windows)
**Prerequisites**
- JDK 17 installed
- MySQL 8 running

**Step 1: Create DB schema**
- Run the SQL in `src/main/resources/db/schema.sql` in MySQL.

**Step 2: Configure DB credentials**
- Edit `src/main/resources/application.properties`:
  - `db.url`
  - `db.username`
  - `db.password`

**Step 3: Build and test**
```powershell
.\mvnw.cmd test
```

**Step 4: Run the JavaFX app**
```powershell
.\mvnw.cmd javafx:run
```

**Console fallback (optional)**
- Run `com.tekup.quiz.ui.ConsoleApp` from your IDE if you want the simple console login/register flow.

## 13) Demo Checklist for Your Presentation
1. Log in as PLAYER and start a quiz.
2. Show the timer and answer submission.
3. Show the result screen and export a result file.
4. Open leaderboard and history.
5. Log in as ADMIN and manage categories.
6. Create or edit a question.
7. Import a CSV and show the validation report.

## 14) Likely Teacher Questions (with answers)
**Q: Why do you separate `dao` and `service`?**
A: DAOs handle persistence only, services handle business rules. This keeps logic clean and testable.

**Q: How is security handled?**
A: Passwords are stored as BCrypt hashes (`PasswordHasher`), never in plain text.

**Q: How is session expiration enforced?**
A: `SessionContext` tracks last activity and `ScreenManager` redirects expired sessions to login.

**Q: What happens if the database is down?**
A: Startup uses `DatabaseManager.verifyConnection()` and shows a clear error screen with the failing config keys.

**Q: How does scoring work?**
A: Each question score is the difficulty weight (EASY=1, MEDIUM=2, HARD=3) if correct.

**Q: How does CSV validation work?**
A: The parser validates column count, question type, correct answer format, and category existence. Errors are reported per row.

---
If you want, I can also generate a short slide outline or a UML sequence diagram for the quiz flow.
