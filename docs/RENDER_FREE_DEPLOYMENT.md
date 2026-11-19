# Render Free deployment guide

This guide deploys the integration-test environment from the `develop` branch. It is not a production deployment.

## 1. Free-plan constraints

Render Free is suitable for a demonstration, with limitations:

- Free web services spin down after 15 minutes without inbound traffic and can take about a minute to wake.
- Free web services have 512 MB RAM and may restart or run out of memory. Keycloak and Spring Boot are both relatively memory-intensive; monitor their deploy/runtime logs.
- A workspace receives 750 Free instance-hours per calendar month. Every running Free web service consumes this shared allowance; two services running continuously would require about 1,440 hours per 30-day month, so the API and Keycloak cannot both remain continuously available on Free. Free services spin down after 15 minutes idle, but real usage can still exhaust the shared allowance.
- A workspace can have only one active Free Render Postgres database. It has 1 GB storage and expires 30 days after creation (with a 14-day upgrade grace period).
- Free web services have ephemeral filesystems, no persistent disks, no shell/SSH access and no horizontal scaling.
- Free web services cannot send outbound SMTP on ports 25, 465 or 587.
- Free services can send private-network requests to a Render datastore in the same region.

Do not use this environment for real student records or production credentials. Back up anything important outside the Free database before its expiry.

## 2. Services to create

Use the same Render region for the database, Keycloak and backend. The Free instance-hour allowance is shared across the workspace, not reserved independently for each service. Running both the API and Keycloak as continuously available Free services is therefore not a viable always-on topology; plan for idle spin-down and limited monthly active time, or use paid compute for one or both services.

| Render resource | Type | Branch / image | Purpose |
| --- | --- | --- | --- |
| Existing Free Postgres, if available | PostgreSQL | Existing instance | Separate schemas for this app and Keycloak |
| `smart-attachment-keycloak` | Web Service (Docker, Free) | Backend repo, `develop`, Dockerfile `Dockerfile.render-keycloak` | OIDC identity provider |
| `smart-attachment-api` | Web Service (Docker, Free) | Backend repo, `develop`, Dockerfile `Dockerfile` | Spring Boot API and existing Thymeleaf pages |
| `smart-attachment-frontend` | Static Site (Free) | Frontend repo, `develop` | React/Vite SPA |

### Important: database isolation and the Free Postgres quota

Use one PostgreSQL instance for **this Smart Attachment environment only**, with separate `smart_attachment` and `keycloak` schemas. Do not reuse a database instance that belongs to an unrelated project (including the Library Management System), even if separate schemas appear technically possible. Shared capacity, credentials, lifecycle, backups and accidental SQL mistakes would couple otherwise independent projects.

The Free plan permits only one active Free Render Postgres instance per workspace. If that quota is already used, do not point this project at another project's database to work around the limit. Use the dedicated PostgreSQL instance already backing this Smart Attachment deployment (if it meets the requirements), choose a separately managed PostgreSQL provider, or upgrade/create an appropriate dedicated database. Record its connection details securely and keep the database and services in the same region when using Render's private network.

## 3. Create isolated PostgreSQL schemas

Connect to the Render Postgres database using its external connection URL from a local PostgreSQL client or pgAdmin. Run this SQL while connected to the database as its owner:

```sql
CREATE SCHEMA IF NOT EXISTS smart_attachment;
CREATE SCHEMA IF NOT EXISTS keycloak;
```

These schemas separate the application's tables from Keycloak's tables. They share the same Free database, storage quota and database instance; this is logical separation, not separate infrastructure or independent backups.

Do not run the SQL against the Library Management System's application schema. Do not paste database URLs or passwords into GitHub issues, PRs or source files.

## 4. Deploy Keycloak

Create a Render **Web Service**:

- Repository: `robinmwaniki/Smart-Attachment_-Internship-Management-Platform`
- Branch: `develop`
- Runtime: Docker
- Dockerfile path: `Dockerfile.render-keycloak`
- Instance type: Free
- Region: same as Postgres
- Service name: `smart-attachment-keycloak`

Set these environment variables in the Render dashboard. Use the Postgres **internal** hostname for the database host.

| Variable | Value |
| --- | --- |
| `KC_DB` | `postgres` |
| `KC_DB_URL_HOST` | Render Postgres internal hostname |
| `KC_DB_URL_PORT` | `5432` |
| `KC_DB_URL_DATABASE` | Render Postgres database name |
| `KC_DB_SCHEMA` | `keycloak` |
| `KC_DB_USERNAME` | Render Postgres username |
| `KC_DB_PASSWORD` | Render Postgres password |
| `KC_BOOTSTRAP_ADMIN_USERNAME` | A new strong admin username |
| `KC_BOOTSTRAP_ADMIN_PASSWORD` | A new strong admin password |
| `KC_HTTP_ENABLED` | `true` |
| `KC_HOSTNAME` | `https://<keycloak-service>.onrender.com` |
| `KC_PROXY_HEADERS` | `xforwarded` |
| `KC_HEALTH_ENABLED` | `true` |

After the service is created, replace the hostname placeholder with the exact HTTPS URL assigned by Render and redeploy. The Keycloak issuer must be exactly:

```text
https://<keycloak-service>.onrender.com/realms/smart-attachment
```

The realm import is included in the Docker image and imported on startup. The imported SPA client initially contains only the local Vite redirect URI. After the frontend Static Site exists, open the Keycloak Admin Console and update client `smart-attachment-frontend`:

- Valid redirect URIs: `https://<frontend-service>.onrender.com/*`
- Web origins: `https://<frontend-service>.onrender.com`
- Post logout redirect URIs: `https://<frontend-service>.onrender.com/*`

Keep the local development URLs too if you still use the local stack. Do not use `*` as a web origin. The client remains public, uses Authorization Code Flow and PKCE S256, and must not have a client secret.

## 5. Deploy the Spring Boot API

Create another Render **Web Service**:

- Repository: `robinmwaniki/Smart-Attachment_-Internship-Management-Platform`
- Branch: `develop`
- Runtime: Docker
- Dockerfile path: `Dockerfile`
- Instance type: Free
- Region: same as Postgres
- Service name: `smart-attachment-api`

Set the following variables. Use the Postgres **internal** hostname, not the external connection URL.

| Variable | Value |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<internal-host>:5432/<database>?currentSchema=smart_attachment` |
| `SPRING_DATASOURCE_USERNAME` | Render Postgres username |
| `SPRING_DATASOURCE_PASSWORD` | Render Postgres password |
| `KEYCLOAK_ISSUER_URI` | `https://<keycloak-service>.onrender.com/realms/smart-attachment` |
| `KEYCLOAK_JWK_SET_URI` | `https://<keycloak-service>.onrender.com/realms/smart-attachment/protocol/openid-connect/certs` |
| `KEYCLOAK_API_AUDIENCE` | `smart-attachment-api` |
| `SPRING_MAIL_HOST` | `localhost` (temporary placeholder only) |
| `SPRING_MAIL_PORT` | `1025` (temporary placeholder only) |
| `SPRING_MAIL_USERNAME` | empty |
| `SPRING_MAIL_PASSWORD` | empty |
| `AFRICASTALKING_API_KEY` | empty until configured |
| `AFRICASTALKING_USERNAME` | `sandbox` |
| `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` | `0.0` for the initial deployment without a Zipkin collector |

Render provides the `PORT` variable; the application already uses `${PORT:8080}`.

**Do not leave the Keycloak JWK URL at localhost.** The public issuer and JWK URL must refer to the same deployed Keycloak service. Spring validates the issuer, signature, expiry and `smart-attachment-api` audience.

The mail values above only allow the app to start without real SMTP credentials; mail sending is not expected to work. Render Free blocks outbound SMTP ports 25, 465 and 587. Use a provider/port supported by Render or defer email testing. Do not add real credentials until you have a working provider configuration.

The current application stores resume bytes in PostgreSQL. This persists across backend restarts, but uploaded files consume the shared 1 GB database capacity. Use small synthetic test files only.

## 6. Deploy the React frontend

Create a Render **Static Site**:

- Repository: `robinmwaniki/smart-attachment-frontend`
- Branch: `develop`
- Build command: `npm ci && npm run build`
- Publish directory: `dist`

Set these public browser configuration variables:

| Variable | Value |
| --- | --- |
| `VITE_API_BASE_URL` | `https://<backend-service>.onrender.com/api` |
| `VITE_KEYCLOAK_URL` | `https://<keycloak-service>.onrender.com` |
| `VITE_KEYCLOAK_REALM` | `smart-attachment` |
| `VITE_KEYCLOAK_CLIENT_ID` | `smart-attachment-frontend` |

All `VITE_*` values are public and are embedded in the built JavaScript. Never put database passwords, Keycloak admin credentials, client secrets or API keys in them.

The frontend API client currently defaults to `/api/v1`, while the backend controllers inspected for this deployment expose routes under `/api` (for example, `/api/applications` and `/api/account/link`). Set the Render value to the backend origin plus `/api`; do not append `/v1` unless a versioned backend route is introduced and verified.

## 7. Deployment order

1. Create or select the Free Postgres instance.
2. Create the `smart_attachment` and `keycloak` schemas.
3. Deploy Keycloak and wait for its public service URL.
4. Deploy the backend with the public Keycloak issuer and JWK URL.
5. Deploy the React Static Site.
6. Add the exact frontend URL to the Keycloak SPA client's redirect, logout and web-origin settings.
7. Trigger a new frontend build if its environment variables were changed after the first deploy.

## 8. Verification checklist

- [ ] Keycloak root URL responds over HTTPS and the realm discovery document loads at `/realms/smart-attachment/.well-known/openid-configuration`.
- [ ] The discovery document's `issuer` exactly matches `KEYCLOAK_ISSUER_URI`.
- [ ] Configure the Spring Boot Render service health-check path as `/actuator/health`; confirm it responds after the service wakes.
- [ ] API logs show a successful PostgreSQL connection and Hibernate schema initialization under `smart_attachment`.
- [ ] Keycloak starts against the `keycloak` schema and imports the `smart-attachment` realm.
- [ ] The React app loads and redirects to the deployed Keycloak login page.
- [ ] A test user can sign in with PKCE and receive the expected realm role.
- [ ] The access token contains audience `smart-attachment-api`.
- [ ] No token or no token returns 401 on a protected API route; a token with the wrong role returns 403.
- [ ] Account linking succeeds only for a test profile with verified email, correct existing password and matching role.
- [ ] A student cannot access another student's application or resume; a company cannot access another company's applications.
- [ ] A test CV upload remains available after the backend service restarts (confirm the record in the database).
- [ ] The Library Management System tables and data remain untouched.
- [ ] Repeat tests after allowing services to spin down and wake up.

Some legacy Thymeleaf routes and un-migrated API routes are intentionally not available through the new SPA flow yet. Email sending, Zipkin export, durable production file storage and production-grade availability are not part of this Free-plan verification.

## 9. Troubleshooting

- **Keycloak returns an invalid issuer error:** compare the token's `iss`, discovery document `issuer`, and backend `KEYCLOAK_ISSUER_URI` character for character.
- **Backend cannot download signing keys:** check that `KEYCLOAK_JWK_SET_URI` is the public HTTPS JWK endpoint, not localhost or a Compose hostname.
- **Keycloak cannot connect to Postgres:** verify the internal hostname, database name, credentials, schema existence, same-region placement and `KC_DB_SCHEMA=keycloak`.
- **Backend creates tables in the wrong schema:** check that the JDBC URL contains `currentSchema=smart_attachment`; do not let it use the shared `public` schema.
- **Login redirects to localhost:** update the SPA client redirect URIs and web origins in the deployed realm, then clear the browser session and retry.
- **Service is slow or unavailable after idle:** allow about a minute for a Free web service to wake up.
- **Service exits or restarts:** inspect Render deploy/runtime logs for memory exhaustion. Free web services have only 512 MB RAM and no SSH/shell access.
- **Postgres is full or expired:** check the shared 1 GB limit and 30-day expiry; export important test data before expiry.

## 10. Security reminders

- Use synthetic student/company accounts and non-sensitive sample CVs only.
- Use unique strong secrets for the Render Keycloak bootstrap admin and database credentials.
- Keep credentials in Render environment variables, never in Git.
- Do not expose the Keycloak admin console credentials to the frontend.
- Do not use the Free environment as a production system or as the only copy of any data.


## 11. Official Render references

Check the current plan limits before deployment because Render can change its Free-plan allowances:

- [Render Free instance and Postgres limitations](https://render.com/docs/free)
- [Render web-service health checks and port configuration](https://render.com/docs/web-services)
