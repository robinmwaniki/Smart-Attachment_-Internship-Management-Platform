# CLAUDE.md

Guidance for Claude Code (or any AI assistant) working in this repository.

## Project overview

Smart Attachment is a Spring Boot web platform that connects students with internships/attachments and lets recruiters post and manage postings. It's a server-rendered Thymeleaf app (not a SPA) with a parallel `/api/**` REST surface, backed by PostgreSQL.

Root package: `com.library.smart_internship`
Group/artifact: `com.library:SmartAttachment`

## Tech stack

- Java 21, Spring Boot 4.1.0 (Maven, wrapper included: `./mvnw`)
- Spring Web MVC + Thymeleaf server-side templates (`src/main/resources/templates`)
- Spring Data JPA + PostgreSQL (`spring.jpa.hibernate.ddl-auto=update` — no migration tool like Flyway/Liquibase is in use)
- Spring Security with JWT (`io.jsonwebtoken` / jjwt 0.12.7) for the API, plus classic form login/session auth for the web UI
- Lombok for entity/DTO boilerplate (`@Getter`/`@Setter`/`@Data`)
- springdoc-openapi for API docs
- Spring Mail (password reset emails) and Africa's Talking SMS integration
- Micrometer + Zipkin tracing, Actuator (`health`, `info` only exposed)
- Docker/`docker-compose.yml`: app + Postgres 17 + Zipkin

## Build, run, test

```bash
./mvnw clean package -DskipTests   # build
./mvnw spring-boot:run             # run locally (needs env vars below)
./mvnw test                        # run tests
docker compose up --build          # full stack: app + postgres + zipkin
```

Required environment variables (see `application.properties`, all read from env, no `.env` committed — `spring-dotenv` is on the classpath so a local `.env` file is picked up automatically):
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRATION` (defaults exist but should be overridden for anything beyond local dev)
- `AFRICASTALKING_API_KEY`, `AFRICASTALKING_USERNAME` (optional — SMS features degrade without them)
- `MANAGEMENT_ZIPKIN_TRACING_ENDPOINT` (optional, defaults to localhost)
- `PORT` (optional, defaults to 8080)

There is no test database config visible — tests currently rely on whatever datasource is configured; be careful running `./mvnw test` against a real DB.

## Architecture

Layering is a fairly standard Spring MVC stack:

```
controller/  -> web (Thymeleaf) + REST endpoints
service/     -> business logic
repository/  -> Spring Data JPA interfaces
entity/      -> JPA entities
dto/         -> request/response payloads
config/      -> SecurityConfig
filter/      -> JwtAuthenticationFilter
handlers/    -> GlobalExceptionHandler
```

### Domain model
- `Student` — single table for both students and recruiters, distinguished by `role` (`STUDENT` / `RECRUITER`). Has `skills` as a comma-separated string, not a normalized table.
- `Internship` — owned by a recruiter (`Student` with role RECRUITER), has `skillsRequired` as a comma-separated string, `slotsAvailable`, `applicationDeadline`, `isActive`.
- `Application` — links a `Student` to an `Internship`, stores the resume as `bytea` directly in Postgres (`resumeData`, `resumeFilename`, `resumeContentType`) rather than on disk/object storage, plus `status`, `feedback`, `interviewDateTime`.
- `PasswordResetToken` — supports the forgot/reset password flow.

### Auth
- `SecurityConfig` wires both form login (session-based, for the Thymeleaf UI) and a `JwtAuthenticationFilter` (for the `/api/**` surface) in the same filter chain.
- Role-based URL rules: `/recruiter/**` and `/api/recruiter/**` require `ROLE_RECRUITER`; `/dashboard`, `/student/**`, `/api/student/**` require `ROLE_STUDENT`.
- `UserDetailsService` loads users by scanning **all** students (`studentRepository.findAll().stream().filter(...)`) rather than a repository query by email — worth fixing if touching auth performance.
- `LoginAttemptService` implements login lockout (15 min) on repeated failures.
- CSRF is disabled globally.

### Matching
- `MatchingService.getRecommendationsForStudent` does a simple skill-overlap match: splits both `skills` and `skillsRequired` comma-separated strings, lowercases/trims, and scores by percentage overlap. No weighting, fuzzy matching, or persistence of match scores — computed on demand.

### Notable simplifications / things to watch for when making changes
- No DB migration tool — schema changes go through Hibernate `ddl-auto=update`, so entity changes alter the live schema on next boot. Be cautious with destructive changes.
- Skills are free-text comma-separated strings on both `Student` and `Internship`, not a normalized/lookup table — matching is exact string comparison after lowercasing.
- Resumes are stored as BLOBs in Postgres, not in the `internship_uploads` Docker volume that docker-compose defines — that volume mount (`/app/uploads`) currently looks unused by the resume flow; verify before assuming file-based storage.
- `pom.xml` has empty `<url/>`, `<licenses>`, `<developers>`, `<scm>` blocks — harmless but worth knowing if generating a proper POM later.

## Conventions to follow when editing

- Match existing Lombok usage (`@Getter/@Setter` on most entities, `@Data` on `Student`) rather than switching styles within a file.
- Controllers are split by concern: `AuthController`/`ApplicationController` (mostly `/api/**`), `StudentController`/`RecruiterController`/`HomeController`/`InternshipController` (mostly Thymeleaf views), `MatchingController` (recommendations), `FileDownloadController` (resume downloads).
- Exceptions should go through `GlobalExceptionHandler` rather than being handled ad hoc in controllers.
- Templates live under `src/main/resources/templates/` (login, register, dashboard, recruiter-dashboard, profile, forgot/reset-password, index) — keep new pages consistent with these names/structure.
- No frontend build tooling (no npm/webpack) — static assets are plain files under `src/main/resources/static/`.

## Per user preference (Robin)
- Provide full file contents inline rather than diffs/snippets when sharing code changes for this Java/Spring project.
- No code comments unless asked.
- Verify Spring Boot API usage against the actual version in `pom.xml` (currently 4.1.0) rather than assuming older Spring Boot 3.x patterns.
- Do not include API key / secret configuration steps in generated code or docs.