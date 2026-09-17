# Database Conventions

## Migrations (Flyway)

- Location: `backend/src/main/resources/db/migration`
- Naming: `V{n}__snake_case_description.sql`, `n` strictly increasing,
  never reused.
- **Never edit an applied migration.** Add a new one instead — Flyway
  checksums applied migrations and fails startup if one changes.
- One logical schema change per migration where practical.
- Migrations run automatically on backend startup
  (`spring.flyway.enabled: true`). No manual `flyway migrate` step
  required for local dev.

## Table conventions (applies from the first domain migration onward)

- Primary keys: `UUID DEFAULT gen_random_uuid()` (via the `pgcrypto`
  extension enabled in `V1__baseline.sql`).
- Every table: `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`,
  `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()`.
- Ownership columns (e.g. `patient_id`) are `NOT NULL` foreign keys,
  indexed — authorization checks depend on these being queryable
  efficiently (see master spec §13, IDOR prevention).
- Never store original + converted values without both columns present
  (unit conversions, per §56) or derived facts without a traceable
  source (§55).
