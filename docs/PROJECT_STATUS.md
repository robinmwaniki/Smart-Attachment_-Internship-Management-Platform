# Project Status and Progress

**Overall status: IN PROGRESS**  
**Last updated: 19 November 2026**  
**Integration branch: `develop`**  
**Deployment: PAUSED**

## Purpose

Build a coordinated attachment and internship management platform for universities, students, workplace supervisors and companies. The system is intended to support opportunity publishing, applications, placement coordination, logbooks, attendance, progress reporting and evaluation.

This remains an academic/portfolio project. It is not production-ready and must not be used to process real student records until authentication, authorization, privacy, operational readiness and end-to-end verification are complete.

## Delivered and merged into develop

| Area | Delivery |
| --- | --- |
| Application authorization | Subject-scoped application history, protected resume access and company status updates; ownership checks are enforced in the backend |
| Database integrity | Unique student/internship application constraint prevents duplicate applications at the database boundary |
| Concurrency | Optimistic locking on application and internship records, with separate-transaction integration tests |
| Outbound resilience | Resilience4j circuit breakers and bounded bulkheads for SMTP and Africa's Talking SMS |
| Provider timeouts | Explicit SMTP connection/read/write and SMS HTTP connect/read timeouts |
| Failure handling | Controlled service-unavailable responses for unavailable providers, open circuits and saturated bulkheads |
| Frontend integration | Separate React SPA with Keycloak initialization, role-aware route handling and public product overview (tracked in frontend repository) |

## In progress

### Keycloak OIDC completion — issue #20

The local Compose topology and realm configuration are present, and the backend validates Keycloak-issued JWTs. The remaining acceptance work is a real local runtime pass:
- Start the Compose stack and confirm Keycloak and its dedicated database become healthy.
- Confirm the imported `smart-attachment` realm and public SPA client are available.
- Obtain browser-issued tokens for each supported role.
- Verify issuer, signature, expiry and API audience validation against the running Keycloak instance.
- Verify 401 for missing/invalid tokens and 403 for authenticated users without the required role.
- Exercise the migrated API routes and confirm that non-migrated routes remain denied.
- Confirm legacy domain profiles remain linked by immutable Keycloak subject, not email.

Mocked-token CI tests alone do not satisfy this runtime acceptance gate.

## Deferred / blocked

- **Sentry (backend issue #26 and frontend issue #7):** waiting for a test-project DSN. Before enabling it, validate event delivery and privacy scrubbing. Secrets must be stored in CI/environment configuration, never committed.
- **Caching (backend issue #28):** deferred until baseline metrics demonstrate a read-heavy, low-volatility candidate. Do not add Redis or cache mutable entity responses speculatively.
- **Render deployment:** intentionally paused. Do not begin deployment preparation or merge deployment PRs until the user explicitly resumes that work.

## Delivery rules

1. Every feature has its own issue and feature branch.
2. Pull requests target `develop`, include a meaningful summary, tests, and documentation updates.
3. Add comments for non-obvious security, concurrency, resilience or data-integrity decisions.
4. Do not merge until all required CI checks pass.
5. Do not promote `develop` to `master` while feature development is active.
6. After each meaningful change, update this file and the README status summary with the new state, PR/issue reference and validation outcome.
7. Never claim production readiness based only on a green build.

## Latest change log

### 19 November 2026
- Closed backend issue #19 after merging authenticated-subject ownership checks for application workflows.
- Closed backend issue #29 after merging database-level duplicate-application protection and optimistic locking.
- Closed backend issue #27 after merging Resilience4j protection for outbound SMTP and SMS integrations.
- Confirmed backend PR #38 passed Maven tests and both Spring Boot and Keycloak Docker image builds before merging to `develop`.
- Confirmed Render deployment remains paused.
