# HELIOS

Health Event & Longitudinal Intelligence Operating System — a
patient-controlled longitudinal health-data platform. See
`docs/ARCHITECTURE.md` for structure and design decisions.

**Status:** Phase 2 — Authentication (P0-01) is fully implemented,
backend and frontend: register/login/logout, JWT-protected endpoints,
real Login/Register pages wired to the API. No other feature modules
yet. See `PHASE_STATUS.md` for what's done and what's next.

## Prerequisites (native, no Docker required)

- Java 21
- Maven 3.9+
- Node 22+ / npm 10+ (repo tested against Node 24 / npm 11)
- Python 3.12+
- PostgreSQL 16+ (tested against 18)
- Git

## First-time setup

### 1. Database

```sql
CREATE DATABASE helios;
CREATE USER helios_app WITH PASSWORD 'changeme';
GRANT ALL PRIVILEGES ON DATABASE helios TO helios_app;
```

On PostgreSQL 15+, database-level privileges alone don't grant rights
inside the `public` schema. Run this too (connected to the `helios`
database):

```sql
GRANT ALL ON SCHEMA public TO helios_app;
```

### 2. Backend

```powershell
cd backend
copy .env.example .env
```

Edit `.env`:
- set `DATABASE_PASSWORD` to your real local DB password
- set `JWT_SECRET` — **required, the app now fails to start without
  it** (fails fast on purpose rather than running with a weak/default
  key). Generate one in PowerShell:
  ```powershell
  [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
  ```
  Paste the output as `JWT_SECRET=...` in `.env`.

> If you already have a `.env` from an earlier phase, it won't have
> `JWT_SECRET` — add the line above to your **existing** `.env` file,
> don't just re-copy `.env.example` over it (that would overwrite your
> real `DATABASE_PASSWORD`).

```powershell
mvn clean install
mvn spring-boot:run
```

`.env` is loaded automatically at startup (via spring-dotenv) — no manual environment-variable export needed.

Verify: `http://localhost:8080/actuator/health` → `{"status":"UP"}`.

Smoke-test auth (in a second terminal, while the backend is running):
```powershell
curl -X POST http://localhost:8080/api/v1/auth/register -H "Content-Type: application/json" -d '{\"email\":\"demo@example.com\",\"password\":\"Sup3rSecret!Pass\",\"role\":\"PATIENT\"}'
curl -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{\"email\":\"demo@example.com\",\"password\":\"Sup3rSecret!Pass\"}'
# copy the "accessToken" value from the login response into TOKEN below
curl http://localhost:8080/api/v1/auth/me -H "Authorization: Bearer TOKEN"
```

### 3. Frontend

```powershell
cd frontend
copy .env.example .env.local
npm install
npm run dev
```

Verify: `http://localhost:5173/register` lets you create an account,
then `http://localhost:5173/login` signs you in and lands on a
placeholder dashboard showing your email/role. The frontend calls the
backend directly at `VITE_API_BASE_URL` (not via a dev proxy), so the
backend must be running with `CORS_ALLOWED_ORIGINS` including
`http://localhost:5173` (already the default in `backend/.env.example`).

### 4. AI service

```powershell
cd ai-service
copy .env.example .env
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Verify: `http://localhost:8000/health` → `{"status":"ok",...}`.

## Running tests

```powershell
cd backend && mvn test
cd ai-service && pytest
```

(Frontend has no tests yet — added when the first real components
exist.)

## Repository layout

```
frontend/     React + TypeScript + Vite
backend/      Java 21 + Spring Boot modular monolith
ai-service/   Python + FastAPI RAG service
docs/         Architecture & ADRs
```

Docker is optional and not provided yet — native development is the
required path per project policy.
