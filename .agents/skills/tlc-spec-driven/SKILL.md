# tlc-spec-driven

## Purpose
Use specification-driven development for every SecondBrain product change. The spec is the source of truth; implementation is complete only when acceptance criteria are verified.

## Trigger
Apply this skill whenever work changes SecondBrain behavior, UX, memory/data, architecture, integrations, build/release behavior, or fixes a bug.

## TLC workflow
TLC = Initialize → Think → Lock → Code → Verify.

### 0. Initialize
Run initialization when a new working session starts, repository/product context is stale or incomplete, `main` changed materially, or work crosses into an uninspected product area.

During initialization:
1. Read root `AGENTS.md` and this skill.
2. Confirm repository, current/base ref, and latest relevant state.
3. Read the current SecondBrain Blueprint Maestro and relevant product-decision docs when available.
4. Inspect active/relevant specs under `docs/specs/`.
5. Inspect current app version/build metadata and relevant Gradle/config metadata.
6. Identify current roadmap focus and affected modules.
7. Check available baseline status (tests/build/CI) when practical.
8. Record constraints, unavailable sources, contradictions, or uncertainties instead of guessing.

Produce a concise TLC Initialization Snapshot containing repository/ref, app version/build, roadmap focus, relevant specs, baseline status, known constraints, and proposed/active spec ID.

### 1. Think
Before editing code:
- Identify the user problem and affected flows.
- Inspect relevant blueprint/spec/code.
- Check cross-screen, persistence, keyboard, date/time, migration, OCR, privacy, crash reporting, and regression impact.
- Define non-goals.

### 2. Lock
Create or update a spec in `docs/specs/` before implementation. A locked spec must include:
- Spec ID and status.
- Problem statement.
- Desired behavior.
- Scope and non-goals.
- Observable acceptance criteria.
- Data/persistence impact.
- UI/UX impact.
- Edge cases/regressions.
- Verification plan.

### 3. Code
Implement the smallest coherent change that satisfies the locked spec. Preserve behavior outside scope, prefer reusable fixes, and add checks/tests where practical.

### 4. Verify
Compare implementation against every acceptance criterion. Record PASS or BLOCKED with evidence. A green build proves compilation, not user-visible correctness.

## SecondBrain-specific verification
- Memory: preserve source/raw capture and provenance.
- Temporal: test ambiguous and multi-date phrases.
- OCR: verify line-item/total/date separation and unrelated-number rejection.
- Adaptive UI: check compact (<360dp), normal (360–839dp), large (≥840dp).
- Crashlytics: CI must still build without `google-services.json`; Firebase-enabled builds must initialize Crashlytics.
- Privacy: do not upload personal content unless the locked spec explicitly authorizes it.

## Definition of Done
A change is Done only when initialization is valid, the spec is locked, all criteria are PASS/BLOCKED with evidence, migration/regression impact is checked, documentation is updated when user-visible, and the PR references the spec.

## Spec lifecycle
`DRAFT → LOCKED → IMPLEMENTING → VERIFYING → DONE`

## Conventions
- Spec IDs: `SB-<AREA>-NNN`.
- Product source of truth: current SecondBrain Blueprint Maestro plus repository product-decision docs.
- Specs live in `docs/specs/`.
- Bug fixes require a regression criterion.
- Do not silently broaden scope.

## Required PR footer
`Spec: SB-AREA-NNN`

Include acceptance criteria with PASS/BLOCKED status.
