# Smart Attachment & Internship Management Platform

A Spring Boot web application designed to digitize student attachment and internship administration. It provides a central workflow for managing student profiles, placement opportunities, applications, supervision, and progress records.

> **Project status: IN PROGRESS.** This is an academic and portfolio project being developed toward a university, student and employer-facing attachment-management platform. Feature integration is active on `develop`; Render deployment is intentionally paused. It is not production-ready and must not be used with real student records until the remaining authentication, privacy, operational and end-to-end checks are completed.
>
> **Latest update — 19 November 2026:** application ownership/IDOR protections, database concurrency safeguards and outbound notification resilience have been merged into `develop`. Keycloak runtime verification and remaining observability work are still in progress or deferred.

## Core workflows

- Student registration and profile management.
- Internship and attachment opportunity management.
- Student applications and application-status tracking.
- Resume/CV document upload.
- Company and placement administration.
- Supervisor assignment and student progress monitoring.
- Daily logbook and attendance records.
- Progress reporting and performance evaluation.

## Technology stack

- Java 21
- Spring Boot 4.1.0
- Spring MVC and Thymeleaf
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL 17
- Keycloak OIDC identity provider (integration in progress)
- Resilience4j circuit breakers and bulkheads for outbound email/SMS
- Spring Security OAuth2 Resource Server
- Spring Mail
- Springdoc OpenAPI
- Micrometer tracing and Zipkin
- Docker and Docker Compose
- Maven

## Architecture overview

The backend uses a layered Spring Boot architecture: controllers expose web/API routes, services enforce application workflows and ownership rules, repositories persist domain data, and PostgreSQL stores application records. The separate React application is the customer-facing SPA; legacy Thymeleaf/session functionality remains during the staged authentication and UI migration.

Docker Compose defines the Spring Boot application, its PostgreSQL database, a separate PostgreSQL database for Keycloak, Keycloak, and Zipkin. Uploaded documents and Keycloak database data are stored in named Docker volumes. Thymeleaf/session authentication remains during this staged migration.

## Prerequisites

- JDK 21 (for running outside Docker)
- Docker Desktop with Docker Compose
- Git

## Run with Docker

1. Clone the repository:
   ```bash
   git clone https://github.com/robinmwaniki/Smart-Attachment_-Internship-Management-Platform.git
   cd Smart-Attachment_-Internship-Management-Platform
   ```
2. Copy `.env.example` to `.env` and replace all placeholder credentials. Keep `.env` private.
3. Start the stack:
   ```bash
   docker compose up --build -d
   ```
4. Open the application at http://localhost:8080.
5. Open Keycloak at http://localhost:8180 and sign in with the local bootstrap administrator credentials from `.env`.
6. Zipkin is available at http://localhost:9411.

Stop the stack:
```bash
docker compose down
```

The database and uploaded files are stored in named Docker volumes. To remove the database and uploaded-file data as well (destructive):
```bash
docker compose down -v
```

## Run with Maven

Configure the required datasource and integration settings in your local environment, then run:
```bash
./mvnw spring-boot:run
```

On Windows:
```powershell
.\mvnw.cmd spring-boot:run
```

## API documentation and health

Springdoc OpenAPI is included for API documentation. Spring Boot Actuator is included for application health and operational endpoints; available paths depend on the application's configuration.

## Configuration notes

- PostgreSQL is exposed on host port `5433` and listens on container port `5432`.
- The application is exposed on port `8080`.
- Keycloak is exposed on port `8180`; its dedicated PostgreSQL database is internal to the Compose network.
- Zipkin is exposed on port `9411`.
- The imported realm is `smart-attachment`; the public SPA client is `smart-attachment-frontend`, configured for Authorization Code Flow with PKCE S256 and no client secret.
- Realm roles are `STUDENT`, `COMPANY`, `SUPERVISOR`, and `ADMIN`. Assign roles to development users in the Keycloak administration console.
- The expected token issuer is `http://localhost:8180/realms/smart-attachment`; the API container retrieves signing keys using the internal Keycloak service address.
- Access tokens must include the `smart-attachment-api` audience. Spring validates the token signature, issuer, expiry, and audience, then maps only the supported realm roles to Spring authorities.
- Existing Thymeleaf pages continue to use the legacy session login during migration. The custom application-issued JWT endpoint and filter have been removed.
- Existing student/recruiter profiles are not rewritten. Their nullable, unique `keycloak_subject` column is populated only through `POST /api/account/link`, using a Keycloak access token with a verified email, the matching existing account password, and a matching realm role. Legacy `RECRUITER` profiles map to Keycloak `COMPANY` without changing the stored role.
- Migrated application routes use the authenticated Keycloak subject for ownership decisions. Other API routes remain restricted until their authorization rules are migrated and verified; frontend route guards are not a security boundary.
- Do not commit `.env`, database passwords, mail credentials, access tokens, or messaging-provider API keys.
- Resume bytes are stored in PostgreSQL; database capacity and backups therefore include uploaded CV data.

## Keycloak local runtime verification

The real Keycloak-backed runtime checks are still outstanding; mocked JWT tests do not prove JWKS retrieval or real token validation. Follow [docs/KEYCLOAK_LOCAL_VERIFICATION.md](docs/KEYCLOAK_LOCAL_VERIFICATION.md) before marking issue #20 complete.

## Current delivery status

| Area | Status | Notes |
| --- | --- | --- |
| Application ownership and IDOR protection | Completed | Subject-scoped application access and protected resume/status operations merged into `develop` |
| Database integrity and concurrency | Completed | Unique student/internship application constraint and optimistic locking with integration tests |
| Outbound notification resilience | Completed | SMTP/SMS circuit breakers, bulkheads, timeouts and controlled failure responses |
| Keycloak OIDC | In progress | Runtime verification against the imported realm and full route authorization checks remain |
| Sentry / observability hardening | Deferred | Requires a test-project DSN and end-to-end privacy validation |
| Read caching | Deferred | Awaiting measured evidence of a safe read-heavy candidate |
| Render deployment | Paused | Deployment is intentionally not part of the current workstream |

See [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md) for the detailed progress log and next steps.

## Render Free deployment

For the separate React frontend, Spring Boot API and Keycloak deployment topology, environment variables, PostgreSQL schema setup, Free-plan limitations and verification checklist, see [docs/RENDER_FREE_DEPLOYMENT.md](docs/RENDER_FREE_DEPLOYMENT.md).

This is a temporary integration-test deployment only. Render Free services are not suitable for production student records.

## Build and test

```bash
./mvnw clean verify
```

## License

No license file is currently documented in this repository.
