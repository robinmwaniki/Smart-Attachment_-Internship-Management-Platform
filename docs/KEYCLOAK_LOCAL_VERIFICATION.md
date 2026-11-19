# Keycloak Local Runtime Verification

**Status: IN PROGRESS — runtime verification has not yet been performed.**  
**Applies to:** local Windows development with Docker Desktop, PowerShell, the Spring Boot API and React frontend.

This runbook closes the gap between CI tests that mock JWT authentication and a real Keycloak-issued access token validated by the running Spring Security Resource Server. Do not mark issue #20 complete until the checks below have been performed against the local Compose stack.

## 1. Start the local stack

From the backend repository root:

~~~powershell
Copy-Item .env.example .env
# Edit .env and replace every placeholder with local-only values.
docker compose up --build -d
docker compose ps
~~~

Do not commit .env. Wait until the application database, Keycloak database and Keycloak containers report healthy. Inspect any unhealthy container before proceeding:

~~~powershell
docker compose logs --tail=200 keycloak keycloak-db app
docker inspect --format='{{.State.Health.Status}}' smart-attachment-keycloak
~~~

The Keycloak container must report healthy.

## 2. Verify realm discovery and the SPA client

~~~powershell
$discovery = Invoke-RestMethod -Uri 'http://localhost:8180/realms/smart-attachment/.well-known/openid-configuration'
$discovery.issuer
$discovery.authorization_endpoint
$discovery.token_endpoint
$discovery.jwks_uri
~~~

Expected issuer:

~~~text
http://localhost:8180/realms/smart-attachment
~~~

Confirm in the Keycloak administration console that:
- Realm smart-attachment is enabled.
- Client smart-attachment-frontend is a public OpenID Connect client.
- Standard Flow is enabled; implicit flow and direct access grants are disabled.
- PKCE method is S256.
- Redirect URI and web origin include the local Vite origin http://localhost:5173.
- Realm roles include STUDENT, COMPANY, SUPERVISOR and ADMIN.
- The access token contains the smart-attachment-api audience.

## 3. Start the frontend and sign in

In the frontend repository, configure its local .env with the Keycloak URL, realm, client ID and API base URL from its .env.example, then run:

~~~powershell
npm install
npm run dev
~~~

Open the local Vite URL, sign in through Keycloak, and use the browser Network panel to inspect the successful authorization-code token exchange. Copy the access token only for the local checks below. Never paste tokens into an issue, pull request, chat, log, screenshot or committed file. Do not enable Keycloak direct-access grants merely to simplify testing.

## 4. Verify the real access token against the API

The following PowerShell helper reads a token without echoing it and checks the HTTP status. It keeps the token in the current PowerShell process only.

~~~powershell
function Get-SecureToken {
    param([string]$Prompt)
    $secure = Read-Host $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        $secure.Dispose()
    }
}

function Get-ApiStatus {
    param([string]$Path, [string]$Token)
    $headers = @{}
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080$Path" -Headers $headers -Method Get
        return [int]$response.StatusCode
    }
    catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            return [int]$_.Exception.Response.StatusCode
        }
        throw
    }
}
~~~

Run the unauthenticated and malformed-token checks:

~~~powershell
Get-ApiStatus -Path '/api/applications/mine' -Token ''
# Expected: 401

Get-ApiStatus -Path '/api/applications/mine' -Token 'not-a-valid-jwt'
# Expected: 401
~~~

Sign in as a test student whose existing application profile has been linked through POST /api/account/link. Obtain that student's real access token through the browser Network panel, then run:

~~~powershell
$studentToken = Get-SecureToken 'Paste the STUDENT access token'
Get-ApiStatus -Path '/api/applications/mine' -Token $studentToken
# Expected: 200 (an empty JSON list is valid when the student has no applications)
~~~

Sign in as a test company account and obtain its real access token:

~~~powershell
$companyToken = Get-SecureToken 'Paste the COMPANY access token'
Get-ApiStatus -Path '/api/applications/mine' -Token $companyToken
# Expected: 403
~~~

The company token is valid but does not have the STUDENT role. This confirms that role authorization is applied after JWT authentication.

Verify that a valid token cannot access an API route that has not yet been migrated:

~~~powershell
Get-ApiStatus -Path '/api/unmigrated-resource' -Token $studentToken
# Expected: 403
~~~

Clear local token variables when finished:

~~~powershell
$studentToken = $null
$companyToken = $null
~~~

## 5. Record the outcome

Record the following in issue #20 (do not include tokens, passwords, client secrets or personal student data):
- Docker Compose health state for the two databases, Keycloak and the API.
- Realm discovery issuer and confirmation that the API audience is present.
- HTTP status results for no token, malformed token, valid STUDENT token, valid COMPANY token and a valid token against a denied route.
- Any failures with sanitized logs and exact container/service names.
- Whether existing domain profiles remained intact after linking by Keycloak subject.

## 6. Acceptance gate

Issue #20 remains IN PROGRESS until:
- the local runtime checks above pass;
- invalid issuer, signature, expiry and missing API audience are rejected (401);
- valid tokens with insufficient roles are rejected (403);
- all four platform realm roles are mapped as expected;
- the relevant API routes have explicit authorization and ownership tests;
- the complete Maven verification and Docker builds pass.

The CI tests that use Spring Security's jwt() test request post-processor exercise authorization rules with a mocked authenticated principal. They do not, by themselves, prove that the running API can retrieve Keycloak JWKS keys or validate a real Keycloak signature, issuer and audience.
