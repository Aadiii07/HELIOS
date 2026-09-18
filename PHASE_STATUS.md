# HELIOS — Project State

- Completed: Phase 0 (scaffolding, verified locally). Phase 1 (Flyway + baseline migration, verified locally). Phase 2 (Authentication/P0-01, backend + frontend) — backend verified locally (`mvn clean test` 8/8 passing against real PostgreSQL); frontend (Login/Register/Dashboard pages, react-router v8, AuthContext, localStorage token persistence) implemented but NOT yet run locally — needs `npm install && npm run dev` confirmation.
- In progress: none
- Tests: backend `mvn clean test` — BUILD SUCCESS, 8/8 passing, confirmed by user against real local PostgreSQL 18. Frontend has no automated tests yet (manual verification only: register → login → dashboard → logout).
- Known issues: none currently open. Fixed during Phase 2 backend verification: AuditService used REQUIRES_NEW propagation, causing a foreign-key violation auditing a just-registered user — changed to REQUIRED so audit writes join the caller's transaction.
- Next phase: Phase 3 — Patient profile (after frontend auth is confirmed working locally)
