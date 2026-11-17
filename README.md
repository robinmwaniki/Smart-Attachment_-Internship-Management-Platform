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
- JWT (JJWT)
- Spring Mail
- Spring Retry
- Springdoc OpenAPI
- Micrometer tracing and Zipkin
- Docker and Docker Compose
- Maven

## Architecture overview

The application uses a layered Spring Boot structure. Controllers handle web requests, services implement application workflows, repositories access persisted data, and PostgreSQL stores application records. Thymeleaf provides the server-rendered user interface.

Docker Compose defines the application, PostgreSQL database, and Zipkin tracing service. Uploaded documents are stored in a named Docker volume.

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
2. Create a local `.env` file based on the variables referenced in `docker-compose.yml`. Configure PostgreSQL, Spring datasource, mail, and optional Africa's Talking settings. Keep credentials and API keys private.
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
- Zipkin is exposed on port `9411`.
- Do not commit `.env`, database passwords, mail credentials, JWT signing secrets, or messaging-provider API keys.
- Verify the configured upload volume and access controls before using real CVs or student records.

## Build and test

```bash
./mvnw clean verify
```

## License

No license file is currently documented in this repository.