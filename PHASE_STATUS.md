# HELIOS — Project State

- Completed: Phase 0-5 (through Document Intelligence) verified end-to-end in the browser. Phase 6 (Structured Observations/P0-05) backend — implemented, NOT yet verified locally, needs `mvn clean test`.
- In progress: none
- Tests: Phase 0-5 confirmed. Phase 6 adds `ObservationFlowTests` (12 cases: manual entry, future-date rejection, candidate-confirm/correct/reject integration, duplicate-review guard, code filtering, cross-patient isolation, role restriction, missing token) — only syntax-checked and cross-referenced in the build sandbox, needs a local run. Expect 49/49 total.
- Known issues: none currently open. Design note: Observation.code is a simple deterministic normalization (uppercase + underscores), explicitly NOT a real clinical coding system (LOINC/SNOMED) — see ADR-011. Found and fixed during this phase's own build (not a shipped gap): reviewing the same candidate twice would have created duplicate Observations — added a CANDIDATE_ALREADY_REVIEWED guard before this ever reached testing.
- Next phase: after Phase 6 backend is confirmed, build its frontend (observations list, manual-entry form), then Phase 7 — Health Timeline (P0-06)
