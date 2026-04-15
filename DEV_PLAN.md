# Java Interactive Quiz - Development Plan

## 1. Locked Decisions
- Java 17+
- JavaFX desktop app
- MySQL 8+ as primary persistence
- CSV used for import/export only
- Authentication: username + password
- Password policy: minimum 6 characters, stored as BCrypt hash
- Categories managed by admin at runtime
- Scoring is weighted by difficulty:
  - EASY = 1
  - MEDIUM = 2
  - HARD = 3
- Quiz length is configurable
- Timer policy: timeout = incorrect answer + auto move to next question
- Leaderboards:
  - Global Top 10
  - Top 10 by category
- Question types in V1:
  - MCQ (4 options)
  - True/False
- Console fallback stays in final delivery

## 2. Architecture
- model: domain entities and enums
- dao: persistence contracts and JDBC implementations
- service: business rules (auth, quiz flow, scoring, stats)
- ui: JavaFX screens and console fallback
- util: db connection, password hashing, config utilities

## 3. Delivery Phases

### Phase 0 - Project Foundation
- Create Maven project and package structure
- Add dependencies (JavaFX, MySQL JDBC, BCrypt, JUnit)
- Add SQL schema and app config template

### Phase 1 - Domain + Contracts
- Implement entities (User, Category, Question hierarchy, QuizAttempt, ScoreRecord)
- Add enums (Role, Difficulty, QuestionType)
- Define DAO interfaces

### Phase 2 - Core Services + Persistence Start
- Implement auth service (register/login)
- Implement user/category/question DAO JDBC start
- Add robust validation and clear domain exceptions

### Phase 3 - Quiz Engine
- Question selection by category + difficulty
- Configurable number of questions
- Timer behavior and auto-advance on timeout
- Weighted score calculation and attempt persistence

### Phase 4 - Results + Leaderboards
- User history and attempt listing
- Global Top 10
- Category Top 10
- Text export for results

### Phase 5 - UI Delivery
- Home screen
- Quiz screen
- Result screen
- Leaderboard screen
- Admin screen (question/category CRUD + CSV import/export)

### Phase 6 - Stabilization
- Unit and integration tests
- Performance checks (<2s common operations)
- Error handling (invalid CSV, DB unavailable, empty question set)
- Packaging and demo script

## 4. Work Split (3-4 Developers)
- Dev A: model + service layer + auth
- Dev B: MySQL schema + JDBC DAOs + integration tests
- Dev C: JavaFX screens + navigation
- Dev D: CSV module + console fallback + QA automation

## 5. Acceptance Criteria
- User can register and login securely
- Admin can fully manage categories/questions
- Player can run a timed quiz with weighted scoring
- Results are saved and visible in history
- Leaderboards work globally and by category
- CSV import/export works with validation
- JavaFX flow is functional and console fallback remains usable
