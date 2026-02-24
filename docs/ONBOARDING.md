# HMS Backend Onboarding

This guide gives a zero-to-running path for local development and API verification.

## 1. Prerequisites

- Java 17+
- MySQL 8+
- Bash
- `curl`

Optional:
- `jq` for pretty JSON output in scripts

## 2. Clone and Enter Project

```bash
git clone <your-repo-url>
cd hms-backend
```

## 3. Configure Environment

The app reads DB and JWT config from environment variables with fallbacks in
`src/main/resources/application.properties`.

Recommended local values:

```bash
export DB_USERNAME=Arashad
export DB_PASSWORD='Arashad@6139'
export JWT_SECRET='VGhpc0lzQVNlY3JldEtleVGhhdElzTG9uZ0Vub3VnaEZvckhTMjU2QWxnb3JpdGht'
export SECURE_COOKIE=false
```

If your local MySQL user/password differ, set `DB_USERNAME` and `DB_PASSWORD` accordingly.

## 4. Start Backend

```bash
./mvnw spring-boot:run
```

Or use the helper script:

```bash
./docs/scripts/local-dev-up.sh
```

What happens on startup:
- Flyway runs SQL migrations from `src/main/resources/db/migration`.
- Seed data is inserted by `DataInitializer` (roles, permissions, departments, and default users).

## 5. Default Seed Users

These users are created if missing:

- `admin` / `admin123`
- `doctor` / `doctor123`
- `nurse` / `nurse123`
- `lab` / `lab123`
- `reception` / `reception123`

## 6. Endpoint Examples (Cookie-based Auth)

This backend sets `HttpOnly` cookies on login (`accessToken`, `refreshToken`).
Use a cookie jar in curl for authenticated requests.

Set base URL:

```bash
BASE_URL=http://localhost:8080
```

### 6.1 Login

```bash
curl -i -X POST "$BASE_URL/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -c cookies.txt \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

### 6.2 Current User (`/me`)

```bash
curl -s "$BASE_URL/api/v1/auth/me" -b cookies.txt
```

### 6.3 Create Patient

```bash
curl -i -X POST "$BASE_URL/api/v1/patients" \
  -H 'Content-Type: application/json' \
  -b cookies.txt \
  -d '{
    "firstName": "Asha",
    "lastName": "Khan",
    "dob": "1992-04-10",
    "gender": "FEMALE",
    "bloodGroup": "B_POSITIVE",
    "contact": "9876543210",
    "email": "asha.khan@example.com",
    "address": "Sector 21",
    "allergies": "Penicillin"
  }'
```

### 6.4 Search Patients

```bash
curl -s "$BASE_URL/api/v1/patients?query=Asha&page=0&size=5" -b cookies.txt
```

### 6.5 Refresh Token

```bash
curl -i -X POST "$BASE_URL/api/v1/auth/refresh-token" -b cookies.txt -c cookies.txt
```

### 6.6 Logout

```bash
curl -i -X POST "$BASE_URL/api/v1/auth/logout" -b cookies.txt -c cookies.txt
```

## 7. Recommended Local Workflow

1. Pull latest code and review migrations in `src/main/resources/db/migration`.
2. Start backend with `./docs/scripts/local-dev-up.sh`.
3. Run `./docs/scripts/smoke-auth-patient.sh` to verify auth + patient flow.
4. Implement feature and tests.
5. Re-run smoke script and relevant tests before commit.

## 8. Useful Commands

Run tests:

```bash
./mvnw test
```

Run a focused test class:

```bash
./mvnw -Dtest=AppointmentServiceTest test
```

## 9. Troubleshooting

- `Access denied for user ...`: verify `DB_USERNAME` and `DB_PASSWORD` env vars.
- `Communications link failure`: MySQL is not running or on wrong port.
- `401/403 on protected endpoints`: repeat login and ensure curl uses `-b cookies.txt`.
- Cookie not set over HTTP in prod-like config: make sure `SECURE_COOKIE=false` locally.
