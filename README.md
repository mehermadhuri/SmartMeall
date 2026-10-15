# SmartMeal

SmartMeal is a campus meal pre-ordering application with a React/Vite frontend, a Java 17/Spring Boot API, MySQL persistence, and a Python forecasting service.

## Project layout

- `frontend/` — student and administrator web application
- `backend/` — Spring Boot REST API, authentication, bookings, and persistence
- `ai-service/` — Flask health and demand-forecast API
- `docker-compose.yml` — local MySQL, backend, and AI service (the frontend runs separately)
- `.github/workflows/ci.yml` — automated checks for pull requests and pushes to `main`/`develop`

## Requirements

- Java 17
- Maven 3.9 or later (there is no Maven wrapper in this repository)
- Node.js 22 or later and npm
- Python 3.11 or later
- Docker Desktop with Docker Compose (optional, for the containerized local stack)

## Local development

### Option A: Run the services with Docker Compose

From the repository root in PowerShell:

```powershell
Copy-Item .env.example .env
```

Edit `.env` and set the blank database passwords and `JWT_SECRET`. The JWT secret must contain at least 32 bytes; use a password manager or generate one locally without sharing it. The example file is a template, not a credential. Then start the backend, local MySQL, and AI service:

```powershell
docker compose up --build
```

The API is at `http://localhost:8080/api`, AI health is at `http://localhost:5001/health`, and MySQL is bound to the local machine only. Start the frontend separately using the instructions below. This Compose configuration is for local development; production deployments must use managed secrets, TLS, and the hosting provider's private networking rather than exposing these local ports.

The backend uses the `production` Spring profile in Compose so it connects to the MySQL container. If both `ADMIN_EMAIL` and `ADMIN_PASSWORD` are configured before the first startup, the backend seeds that initial administrator. There is no default admin account.

### Option B: Run backend and AI service directly

The default Spring profile uses an in-memory H2 database for local development. Set a local JWT secret before starting the backend. In PowerShell, this creates a random secret in the current shell without printing it:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
Set-Location backend
mvn spring-boot:run
```

To use MySQL/Aiven instead of H2, set `SPRING_PROFILES_ACTIVE=production` and provide the database and CORS variables described below. Do not put production credentials in source files.

Run the AI service in another terminal:

```powershell
Set-Location ai-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements-dev.txt
python forecast_service.py
```

Run the frontend in another terminal:

```powershell
Copy-Item frontend/.env.example frontend/.env.local
Set-Location frontend
npm ci
npm run dev
```

The frontend defaults to `http://localhost:8080/api`; set `VITE_API_BASE_URL` in `frontend/.env.local` if the backend is hosted elsewhere. Vite embeds this value at build time.

## Environment configuration

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Use `production` for MySQL/Aiven; local default is H2. |
| `DB_URL` | JDBC URL. For Aiven use its MySQL host/port/database and `sslMode=REQUIRED`; for local Compose the example URL uses the `db` service. |
| `DB_USERNAME`, `DB_PASSWORD` | Database credentials. |
| `MYSQL_DATABASE`, `MYSQL_ROOT_PASSWORD` | Local Compose database initialization only. |
| `JWT_SECRET` | Private signing key of at least 32 bytes; required for backend startup. |
| `JWT_EXPIRATION_MS` | JWT lifetime in milliseconds (default 24 hours). |
| `CORS_ALLOWED_ORIGINS` | Comma-separated exact browser origins. Set the deployed frontend origin; do not use `*`. |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Optional initial administrator seed. Set both securely; the password must be at least 12 characters. No default credentials are provided. |
| `BOOKING_CUTOFF_MINUTES` | Booking change/cancellation cutoff before pickup (default 30 minutes). |
| `AI_SERVICE_URL` | Base URL for the forecasting service (`http://ai-service:5001` inside Compose). |
| `AI_SERVICE_CONNECT_TIMEOUT_MS`, `AI_SERVICE_READ_TIMEOUT_MS` | Backend timeouts for the AI request. |
| `VITE_API_BASE_URL` | Frontend API base URL, including `/api`. |

For Aiven, store database credentials and the CA/TLS configuration in your hosting provider's secret/configuration settings. Aiven connection requirements vary by service setup; verify the generated JDBC URL and certificate requirements in the Aiven console. Never commit `.env` or `.env.local` files.

## Tests and build checks

Run these from the repository root:

```powershell
Set-Location backend
mvn verify

Set-Location ../frontend
npm ci
npm test
npm run lint
npm run build

Set-Location ../ai-service
python -m pip install -r requirements-dev.txt
python -m pytest -q
```

The backend integration tests use an isolated H2 test profile and do not require Aiven credentials. Frontend tests mock API calls. AI tests use Flask's test client and do not call external services.

## GitHub Actions

The CI workflow runs backend Maven verification, frontend tests/lint/build, and AI-service tests on pushes and pull requests targeting `main` or `develop`. It needs no deployment secrets. A successful local run does not prove the GitHub-hosted workflow has run; verify its status in the repository's Actions tab after pushing.

## Deployment preparation

No production database or hosting account is configured by this repository, and no public deployment URL is claimed. A practical low-cost layout is a static frontend host (for example, Cloudflare Pages) plus container hosting for the Spring Boot API and Python AI service, with Aiven MySQL. Compare current pricing, region, TLS, networking, and free-tier limits before choosing a provider; free tiers and availability change.

For a real deployment:

1. Create the Aiven MySQL service and database. Copy its connection details into the hosting provider's secret store; require TLS.
2. Build and deploy `backend/` and `ai-service/` as separate containers, or use a platform that supports both runtimes. Set `SPRING_PROFILES_ACTIVE=production`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, and `AI_SERVICE_URL` as provider-managed settings.
3. Deploy `frontend/` as a Vite static site with `VITE_API_BASE_URL` set to the API's HTTPS URL plus `/api`, then set the API's `CORS_ALLOWED_ORIGINS` to the exact frontend HTTPS origin.
4. Configure an initial administrator using provider-managed `ADMIN_EMAIL` and `ADMIN_PASSWORD` settings before the initial seed, then remove the password setting when no longer needed. The app does not include a default admin login.
5. Check `GET /api/health` on the API and `GET /health` on the AI service, then exercise registration, login, and booking against the deployed database. Only publish URLs after those checks pass.

Review database schema migration strategy and backups before production use. The current application uses Hibernate schema update and has no versioned migration scripts yet.
