# Interactive Quiz (JavaFX + MySQL)

An interactive quiz desktop application with JavaFX UI and a console fallback. Players can register, take timed quizzes, view results and leaderboards, while admins manage categories/questions and import/export CSV data.

## Features
- Player: register/login, timed quiz, results, leaderboard, history
- Admin: categories/questions CRUD, CSV import/export with row-level validation
- MySQL persistence with pooled connections (HikariCP)
- BCrypt password hashing
- Structured logging for key flows

## Tech stack
- Java 17 (JDK)
- Maven Wrapper
- JavaFX 21
- MySQL 8+
- HikariCP, BCrypt, SLF4J
- JUnit 5 + H2 for tests

## Setup from A to Z

### 1) Prerequisites
Install:
- JDK 17 (not JRE)
- MySQL 8+

Verify:
```bash
java -version
javac -version
```

### 2) Clone the repository
```bash
git clone https://github.com/Souhaib55/Java_Project.git
cd Java_Project
```

### 3) Create the database schema
1. Start MySQL.
2. Run the SQL script:
   - `src/main/resources/db/schema.sql`

This creates tables and inserts seed categories.

### 4) Configure runtime settings
Edit `src/main/resources/application.properties`:

Required:
- `db.url`
- `db.username`
- `db.password`

Optional:
- `db.pool.*` (HikariCP tuning)
- `quiz.defaultQuestionCount`
- `quiz.secondsPerQuestion`
- `session.timeoutMinutes`

Environment overrides are supported by `AppConfig` (for example `DB_URL`).

### 5) Build and test
Windows:
```powershell
./mvnw.cmd test
```

Linux/macOS:
```bash
./mvnw test
```

### 6) Run the JavaFX app
Windows:
```powershell
./mvnw.cmd javafx:run
```

Linux/macOS:
```bash
./mvnw javafx:run
```

### 7) Console fallback (optional)
Run `com.tekup.quiz.ui.ConsoleApp` from your IDE for the text-based login/register flow.

## Project structure
```
projetjava/
  src/main/java/          Application code
  src/main/resources/     application.properties, db/schema.sql
  src/test/java/          Unit + integration tests
  src/test/resources/     H2 test config
  pom.xml                 Dependencies and build config
  mvnw, mvnw.cmd          Maven Wrapper
```

## Testing
- Full suite (Windows): `./mvnw.cmd test`
- Full suite (Linux/macOS): `./mvnw test`

Integration tests use H2 and the config in `src/test/resources/application.properties`.

## CI
- Workflow: `.github/workflows/ci.yml`
- Command: `./mvnw -B -ntp verify`

## Troubleshooting
- **"No compiler is provided in this environment"**
  - Install JDK 17 and ensure `JAVA_HOME` points to it.
- **DB connection errors**
  - Verify MySQL is running and credentials in `application.properties`.
- **Table not found in tests**
  - Ensure `IntegrationTestDatabase.resetSchema()` runs in DAO tests.

## Runbooks
- Testing setup: `SETUP_TESTING.md`
- Build and deploy: `BUILD_AND_DEPLOY.md`
- Project deep-dive: `JavaProjectExplanation.md`
