# Interactive Quiz (Java)

Initial implementation bootstrap for the Java interactive quiz project.

## Tech stack
- Java 17+ (JDK)
- Maven
- JavaFX
- MySQL 8+
- BCrypt password hashing

## Run checks
- Build and tests (Windows): mvnw.cmd test
- Build and tests (Linux/macOS): ./mvnw test
- Run JavaFX app (Windows): mvnw.cmd javafx:run
- Run JavaFX app (Linux/macOS): ./mvnw javafx:run

## CI quality gate
- GitHub Actions workflow: .github/workflows/ci.yml
- Trigger: push and pull_request
- Command executed in CI: ./mvnw -B -ntp verify

## Runbooks
- Testing setup: SETUP_TESTING.md
- Build and deploy: BUILD_AND_DEPLOY.md

## Database setup
- Execute the schema file before running the app:
	- src/main/resources/db/schema.sql
- Ensure application.properties has valid MySQL credentials.

## Database pool settings
- Pooled DB access is enabled through HikariCP.
- Optional tuning keys in src/main/resources/application.properties:
	- db.pool.maxSize
	- db.pool.minIdle
	- db.pool.connectionTimeoutMs
	- db.pool.validationTimeoutMs
	- db.pool.idleTimeoutMs
	- db.pool.maxLifetimeMs

## Session safety
- Session inactivity timeout is enabled with key:
	- session.timeoutMinutes
- Expired sessions are redirected to authentication with a clear timeout message.

## Runtime observability
- Structured service/DAO logging is enabled for:
	- Authentication attempts and outcomes
	- Quiz generation/evaluation lifecycle
	- Leaderboard and history data loading paths
	- Result export lifecycle
	- CSV import/export summaries and row-level rejects
	- Admin action audit trails (category/question/CSV operations)
	- Screen-level load and recovery failures (history/leaderboard/result/admin CSV)
	- Database pool initialization and connection verification

## Current status
- Project structure initialized
- Domain model and DAO contracts implemented
- JDBC implementations started (users, categories, questions, attempts) with admin listing/delete support
- Auth, quiz, leaderboard, history, CSV import/export services started
- Stabilization started: H2-backed DAO integration tests and CSV service edge-case tests added
- Reliability hardening started: Maven Wrapper and startup DB/config failure handling added
- Persistence hardening started: pooled DB connections, optimized random question retrieval, and stronger CSV parsing/validation added
- Release safety hardening started: shared form validation, session timeout guardrails, and structured logging added
- JavaFX navigation foundation implemented
- JavaFX flow implemented: Authentication -> Category + Quiz Config -> Timed Quiz -> Result -> Leaderboard/History
- ADMIN flow implemented: Dashboard -> Category Management -> Question Management -> CSV Tools
- ADMIN routes are guarded and hidden for PLAYER users
- Console fallback auth flow available
