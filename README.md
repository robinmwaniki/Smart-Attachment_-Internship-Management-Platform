# Smart Attachment & Internship Management Platform

A Spring Boot web application designed to digitize student attachment and internship administration. It provides a central workflow for managing student profiles, placement opportunities, applications, supervision, and progress records.

> **Project status:** Academic and portfolio project intended for local development and demonstration. Review configuration and security before any real-world deployment or use of personal data.

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
- Keycloak OIDC identity provider (migration in progress)\n- Spring Security OAuth2 Resource Server
- Spring Mail
- Spring Retry
- Springdoc OpenAPI
- Micrometer tracing and Zipkin
- Docker and Docker Compose
- Maven

## Architecture overview

The application uses a layered Spring Boot structure. Controllers handle web requests, services implement application workflows, repositories access persisted data, and PostgreSQL stores application records. Thymeleaf provides the server-rendered user interface.

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
5. Zipkin is available at http://localhost:9411.

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
- Keycloak is exposed on port `8180`; its dedicated PostgreSQL database is internal to the Compose network.\n- Zipkin is exposed on port `9411`.\n- The imported realm is `smart-attachment`; the public SPA client is `smart-attachment-frontend`, configured for Authorization Code Flow with PKCE S256 and no client secret.\n- Realm roles are `STUDENT`, `COMPANY`, `SUPERVISOR`, and `ADMIN`. Assign roles to development users in the Keycloak administration console.\n- The expected token issuer is `http://localhost:8180/realms/smart-attachment`; the API container retrieves signing keys using the internal Keycloak service address.
- Do not commit `.env`, database passwords, mail credentials, JWT signing secrets, or messaging-provider API keys.
- Verify the configured upload volume and access controls before using real CVs or student records.

## Build and test

```bash
./mvnw clean verify
```

## License

No license file is currently documented in this repository.