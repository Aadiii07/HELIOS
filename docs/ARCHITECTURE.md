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

### ADR-009: Local-filesystem object storage for now, S3 interface ready
`ObjectStorageService` is the storage abstraction; `LocalFilesystemStorageService`
is a real (not mocked) implementation that satisfies native-dev
requirements (§10 — Docker/external services never required to run
the app). No AWS SDK dependency has been added, since there is no S3
endpoint to actually test against yet — adding one later means
implementing the same interface, not restructuring the module.

### ADR-010: PDF text extraction only; no OCR yet
`TextExtractor` is the abstraction; `PdfTextExtractor` (Apache PDFBox,
pure Java) is the only implementation. Image documents (JPG/PNG) are
marked `UNSUPPORTED_FORMAT` rather than faking an extraction result.
Real OCR needs a native dependency (e.g. Tesseract via Tess4j) that
this project hasn't added — given how much native/external-tool
friction this project has already hit in Windows setup, adding one
without a way to verify it works is worse than being explicit about
the gap. A clean extension point (implement `TextExtractor`) is
ready for when that's actually needed.

`LabValueCandidateExtractor`'s line parser was originally column-gap
based (required 2+ consecutive spaces between fields), which missed
real-world PDFs using single-space-separated columns — found via
user testing with an actual lab-report PDF, not a hypothetical.
Rewritten to parse tokens right-to-left (reference range, then unit
if preceded by a number, then the numeric value, everything else is
the label) so it no longer depends on how many spaces a given PDF
generator happens to use. Still an explicitly best-effort heuristic,
not a general lab-report parser — many layouts will still miss.

### ADR-011: Observation.code is a deterministic normalization, not a real coding system
`CodeNormalizer` turns a display name into an uppercase, underscore-
separated code (e.g. "Hemoglobin A1c" → "HEMOGLOBIN_A1C") purely so
the same measurement typed or extracted with different casing/
spacing groups together. This is NOT a LOINC/SNOMED mapping — real
clinical coding is a distinct, much larger effort for the
FHIR/Interoperability phase (master spec §45), not implied or faked
here. An Observation is only ever created from a patient-confirmed/
corrected extraction candidate or direct manual entry — there is no
auto-accepted path, and confidence is always HIGH by construction
(a human verified the value; the original extraction confidence,
which may have been MEDIUM, is a separate, preserved fact on the
ExtractionCandidate it came from).

### ADR-012: Change-password added to the identity module (not a new module)
`POST /api/v1/auth/change-password` — authenticated, requires the
current password, enforces the same policy as registration (shared
via `PasswordPolicy`, so the two can't drift apart). Known,
documented limitation shared with logout (ADR-006): this does not
invalidate JWTs issued before the change — stateless tokens remain
valid until they expire naturally. A revocation/blocklist for
immediate invalidation is the same P1 backlog item logout already
notes, not a new gap.

## Status

Phase 0: repository scaffolding, build configuration, empty
entrypoints for all three components — verified working locally.

Phase 1: Flyway-managed migrations wired in; baseline migration only
(extensions) — verified working locally against real PostgreSQL.

Phase 2: Authentication (P0-01) — backend `identity` module
(User/AuditEvent entities, register/login/logout/me, JWT, BCrypt,
password policy, role-restricted self-registration, in-memory login
rate limiting, global error-response format) and frontend
(Login/Register/Dashboard pages, react-router v8, AuthContext with
localStorage token persistence) — verified working end-to-end locally
(`mvn clean test` 9/9 passing; register → login → protected dashboard
→ logout confirmed in the browser).

Phase 3 (this commit): Patient Profile (P0-02) — backend `patient`
module (name, date of birth, phone, address, preferred language),
scoped to PATIENT-role accounts only (`@PreAuthorize("hasRole('PATIENT')")`,
method security enabled). No client-supplied ID anywhere in this
API — every request resolves "my profile" from the authenticated
JWT, which is what rules out IDOR here by construction rather than
by a checked ownership field. Backend verified locally (`mvn clean
test`, 17/17 passing). Frontend: a Profile view/edit page (create-vs-
update handled by the same form, based on whether GET returns 404
PROFILE_NOT_FOUND) — verified working in the browser (including a
layout/redirect polish pass).

Phase 4: Document Vault (P0-03) — backend `documents` module (upload
with magic-byte content validation against a PDF/JPG/PNG allow-list,
paginated list, metadata, content download, soft-delete — all scoped
to PATIENT-role accounts and the requester's own documents only)
verified locally (`mvn clean test`, 29/29 passing). Storage is a
clean interface (`ObjectStorageService`) with a real local-filesystem
implementation for native dev (see ADR-009) — no S3 dependency added
yet. Frontend: a Documents page (upload, list, download via Blob +
synthetic anchor click, delete with confirmation) — verified working
in the browser.

Phase 5: Document Intelligence (P0-04) — backend extraction (see
ADR-010: PDF only, real Apache PDFBox text extraction; images marked
UNSUPPORTED_FORMAT). A deterministic, rule-based parser
(`LabValueCandidateExtractor` — explicitly not ML/LLM-based, and
rewritten mid-phase after user testing found it missed real-world
single-space-separated PDFs) looks for lab-report-shaped lines and
produces `ExtractionCandidate` rows, always `PENDING` review —
nothing is auto-trusted into a confirmed fact (master spec §16).
Frontend: an extraction-status badge per document, and a review page
(Confirm/Reject/Correct per candidate). Verified working end-to-end
in the browser with a real user-supplied lab-report PDF.

Phase 6 (this commit): Structured Observations (P0-05) backend —
`observations` module. An `Observation` is created only from a
patient's CONFIRMED/CORRECTED review of an extraction candidate, or
direct manual entry — never automatically from extraction alone.
Guards against double-creating an Observation if the same candidate
is reviewed twice (a `CANDIDATE_ALREADY_REVIEWED` conflict — found
and fixed while building this integration, not shipped as a known
gap). The model is deliberately flexible (`code`/`displayName` are
free text, not a fixed enum of test types — see ADR-011) with a
`numericValue` parsed opportunistically alongside the preserved raw
string, so future trend/timeline features have something to sort and
compare without losing the original value. Not yet verified locally.

Also this commit: change-password added to the identity module (see
ADR-012) — user-requested, out of phase order but a natural fit for
P0-01's existing scope. Not yet verified locally.
