# SmartMeal

SmartMeal is a campus meal pre-ordering application with a React/Vite frontend, a Java 17/Spring Boot API, MySQL persistence, and a Python forecasting service.

## Project links

| Project component | Link |
| --- | --- |
| GitHub repository | [mehermadhuri/SmartMeall](https://github.com/mehermadhuri/SmartMeall) |
| React + Vite frontend source | [frontend/](https://github.com/mehermadhuri/SmartMeall/tree/master/frontend) |
| Java 17 + Spring Boot backend source | [backend/](https://github.com/mehermadhuri/SmartMeall/tree/master/backend) |
| Python AI forecasting service source | [ai-service/](https://github.com/mehermadhuri/SmartMeall/tree/master/ai-service) |
| Render deployment configuration | [render.yaml](https://github.com/mehermadhuri/SmartMeall/blob/master/render.yaml) |
| Vercel frontend configuration | [frontend/vercel.json](https://github.com/mehermadhuri/SmartMeall/blob/master/frontend/vercel.json) |
| GitHub Actions CI workflow | [ci.yml](https://github.com/mehermadhuri/SmartMeall/blob/master/.github/workflows/ci.yml) |
| Local multi-service setup | [docker-compose.yml](https://github.com/mehermadhuri/SmartMeall/blob/master/docker-compose.yml) |

## Deployment links

| Service | Link | Status |
| --- | --- | --- |
| Vercel React frontend candidate | [smartmeal-frontend.vercel.app](https://smartmeal-frontend.vercel.app/) | Not this repository's React client; the Vercel project must be connected to this repository's `frontend/` directory and redeployed. |
| Render backend candidate | [smartmeal-backend-l5xs.onrender.com](https://smartmeal-backend-l5xs.onrender.com/) | Previous health request timed out. A newly created Render Blueprint service may have a different URL. |
| Backend health check | [API `/api/health`](https://smartmeal-backend-l5xs.onrender.com/api/health) | Not verified. |
| Render AI service | URL not yet available | Render must create the service from `render.yaml`; use its deployed URL after creation and health verification. |
| Aiven MySQL | No public link | Connection details are private and must be configured in Render's secret settings. |

The Vercel and Render addresses above are candidates, not proof that this repository is deployed. Replace them with the actual deployment URLs after connecting the provider projects, deploying, and verifying the frontend, API, AI health endpoint, and application flows.

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
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:DB_URL = 'jdbc:h2:mem:smartmeal;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
$env:DB_USERNAME = 'sa'
$env:DB_PASSWORD = ''
$env:DB_DRIVER = 'org.h2.Driver'
$env:HIBERNATE_DIALECT = 'org.hibernate.dialect.H2Dialect'
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

The current deployment candidates and their verification status are listed in [Deployment links](#deployment-links) above. They are not proof that this source is live. Provider dashboards are required to connect the Vercel project to this repository's `frontend/` directory and retrieve Render's generated service URLs; update the table after deploying and verifying the application.

For a real deployment:

1. Create the Aiven MySQL service and database. Copy its connection details into the hosting provider's secret store; require TLS.
2. In Render, create a Blueprint deployment from this repository's root `render.yaml`. Set `DB_URL` to Aiven's JDBC URL with TLS required, and set `DB_USERNAME` and `DB_PASSWORD` through Render's secret settings. The Blueprint generates `JWT_SECRET` and links the backend to the AI service's HTTPS hostname. Render's free web services cannot receive private-network connections, so the AI service is reached through its public HTTPS endpoint.
3. In Vercel, import this repository and set the project's **Root Directory** to `frontend`. Set the `VITE_API_BASE_URL` environment variable to `https://<your-render-backend-host>/api` for each deployment environment, then deploy. `frontend/vercel.json` provides the SPA route fallback.
4. Set Render's `CORS_ALLOWED_ORIGINS` to the exact deployed Vercel origin (for example, `https://<your-vercel-project>.vercel.app`), without a trailing path or wildcard, then redeploy the backend.
5. Configure an initial administrator using provider-managed `ADMIN_EMAIL` and `ADMIN_PASSWORD` settings before the initial seed, then remove the password setting when no longer needed. The app does not include a default admin login.
6. Check `GET /api/health` on the API and `GET /health` on the AI service, then exercise registration, login, and booking against the deployed database. Only treat the public URLs as live after those checks pass.
7. In the README link table, replace the candidate URLs with the actual deployed URLs. The existing Vercel URL must be reassigned from its unrelated project to this frontend; the old Render URL does not automatically become the new Blueprint service's URL.

Review database schema migration strategy and backups before production use. The current application uses Hibernate schema update and has no versioned migration scripts yet.
