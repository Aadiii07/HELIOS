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

## Status

Phase 0 (this commit): repository scaffolding, build configuration,
and empty entrypoints for all three components. No domain logic,
no database schema, no authentication — see backend module list
above for what's still to come.
