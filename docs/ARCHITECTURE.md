# HELIOS Architecture

## Overview

Three top-level components, developed native-first (no Docker requirement):

```
HELIOS/
├── frontend/    React + TypeScript + Vite
├── backend/     Java 21 + Spring Boot (modular monolith)
├── ai-service/  Python + FastAPI (RAG assistant, separate deployable)
└── docs/
```

`backend` is a single deployable modular monolith. Planned internal
module boundaries (introduced incrementally, not yet implemented):
identity, patient, documents, observations, timeline, analytics,
evidence, consent, provider, audit, export, interoperability,
notifications, admin.

`ai-service` is split out physically (not just logically) because it
has a different runtime (Python), different dependency profile
(LLM/vector tooling), and different scaling characteristics than the
JVM backend. Everything else stays in the monolith until a concrete
requirement forces a split (see ADR-001).

## Data flow (target, not yet implemented)

```
Patient → frontend → backend REST API → PostgreSQL
                                       → object storage (documents)
                    → ai-service (RAG) → backend (authorized read) → PostgreSQL
```

Authorization is always checked in the backend before the ai-service
is allowed to retrieve any patient data (see master spec §24, RAG
Security — implemented in Phase 11).

## ADR Log

### ADR-001: Modular monolith, not microservices
Single Spring Boot deployable for all backend domains. Avoids
premature distributed-systems complexity. Revisit only if a module
demonstrates independent scaling/deployment needs.

### ADR-002: PostgreSQL as the only datastore for Phase 0–P0
One relational database. Redis and a vector store are added only when
a specific phase requires them (caching; RAG embeddings).

### ADR-003: Native-first development
`mvn spring-boot:run`, `npm run dev`, and `uvicorn` must each work
directly on the developer's machine. Docker, if added later, is an
optional convenience layer, never a requirement.

### ADR-004: AI service physically separated from backend
Different runtime and dependency lifecycle (Python/FastAPI vs.
Java/Spring). Communicates with the backend over HTTP using
backend-issued authorization, never with direct database access.

### ADR-005: Flyway for schema migrations
Versioned SQL migrations own the schema; Hibernate `ddl-auto` stays
`none`/`validate`, never `update`/`create`. Guarantees the schema
history is explicit and reviewable (see docs/DATABASE.md).

### ADR-006: Stateless JWT, no session store
Access tokens only (HS256, short-lived). No refresh token, no
server-side revocation/blocklist yet — logging out relies on the
client discarding the token. A shared revocation store (Redis) is a
P1 backlog item if immediate/"log out everywhere" revocation becomes
a real requirement.

### ADR-007: In-memory login rate limiting (single instance only)
Failed-login tracking lives in a JVM `ConcurrentHashMap`, not a shared
store. Correct for one backend instance; a multi-instance deployment
needs a shared store (Redis) instead — not built now (§50, no
overengineering ahead of an actual requirement).

### ADR-008: JWT stored in localStorage, not an httpOnly cookie
Matches the backend's stateless Bearer-token design and needs no CSRF
handling. Trade-off: readable by any script on the page, so it's only
as safe as the frontend's own resistance to XSS. An httpOnly-cookie
session is the P1 hardening path if that trade-off stops being
acceptable — not built now.

## Status

Phase 0: repository scaffolding, build configuration, empty
entrypoints for all three components — verified working locally.

Phase 1: Flyway-managed migrations wired in; baseline migration only
(extensions) — verified working locally against real PostgreSQL.

Phase 2 (this commit): Authentication (P0-01) — backend `identity`
module (User/AuditEvent entities, register/login/logout/me, JWT,
BCrypt, password policy, role-restricted self-registration, in-memory
login rate limiting, global error-response format) verified working
locally (`mvn clean test`, 8/8 passing against real PostgreSQL).
Frontend: real Login/Register/Dashboard pages (react-router v8
declarative mode, AuthContext with localStorage token persistence),
wired directly to the backend API — not yet run locally.
