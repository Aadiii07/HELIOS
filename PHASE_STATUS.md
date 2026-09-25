# HELIOS — Project State

- Completed: Phase 0-5 verified end-to-end in the browser. Phase 6 (Structured Observations/P0-05) backend — implemented, NOT yet verified locally. Change-password endpoint (user-requested, added to identity module) — implemented, NOT yet verified locally.
- In progress: none
- Tests: Phase 0-5 confirmed. Phase 6 adds ObservationFlowTests (12 cases). Change-password adds 4 cases to AuthenticationFlowTests (now 11 total in that file). Combined expected total: 53 (49 from Phase 6 + 4 new). Not yet run — needs `mvn clean test`.
- Known issues: none currently open. Change-password known limitation (documented, not a gap): does not invalidate JWTs issued before the change — same stateless-token trade-off as logout (ADR-006/ADR-012).
- Next phase: confirm `mvn clean test` (Phase 6 + change-password together), then build Phase 6 frontend, then Phase 7 — Health Timeline (P0-06)
