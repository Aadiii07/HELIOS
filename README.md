# HELIOS

Health Event & Longitudinal Intelligence Operating System — a
patient-controlled longitudinal health-data platform. See
`docs/ARCHITECTURE.md` for structure and design decisions.

**Status:** Phase 0 — repository foundation only. No features
implemented yet (no auth, no data model, no UI beyond a placeholder
page). See `PHASE_STATUS.md` for what's done and what's next.

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

### 2. Backend

```powershell
cd backend
copy .env.example .env
# edit .env with your real local DB password, then export as
# environment variables before running (Spring Boot does not load
# .env files itself), e.g. in PowerShell:
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#=]+)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2])
    }
}
mvn clean install
mvn spring-boot:run
```

Verify: `http://localhost:8080/actuator/health` → `{"status":"UP"}`.

### 3. Frontend

```powershell
cd frontend
copy .env.example .env.local
npm install
npm run dev
```

Verify: `http://localhost:5173` renders the HELIOS placeholder page.

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
