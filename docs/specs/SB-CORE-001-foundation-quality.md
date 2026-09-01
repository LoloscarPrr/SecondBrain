# SB-CORE-001 — Foundation quality: Crashlytics, adaptive layouts, skills

Status: IMPLEMENTING

## Problem
SecondBrain needs stronger production diagnostics, adaptive UI behavior across screen sizes, and a repository workflow that prevents context loss and unverified changes as the project grows.

## Desired behavior
- The app compiles without Firebase credentials in CI/local development.
- When `app/google-services.json` is present, Firebase/Crashlytics plugins activate and the app initializes Crashlytics.
- The main screen adapts through compact, normal, and large layouts without duplicating product logic.
- Repository agents follow the TLC spec-driven skill before changing behavior.

## Scope
- Firebase Crashlytics SDK and conditional Gradle integration.
- Crash reporting bridge initialized from `Application`.
- Compact `<360dp`, normal `360–839dp`, large `≥840dp` layouts.
- TLC skill, AGENTS instructions, spec lifecycle/template.

## Non-goals
- Creating the Firebase project or manufacturing `google-services.json`.
- Redesigning SecondBrain visual identity.
- Adding new memory-engine behavior.

## Acceptance criteria
- [ ] AC1 — CI assembles the debug APK without `google-services.json`.
- [ ] AC2 — Firebase plugin configuration is automatically enabled when `app/google-services.json` exists.
- [ ] AC3 — CrashReporter initializes safely and does not break offline/local use when Firebase is unavailable.
- [ ] AC4 — Compact layout remains usable below 360dp.
- [ ] AC5 — Normal layout remains centered/readable from 360dp through 839dp.
- [ ] AC6 — Large layout uses a constrained two-column composition at 840dp and above.
- [ ] AC7 — `.agents/skills/tlc-spec-driven/SKILL.md`, root `AGENTS.md`, and `docs/specs/` workflow exist.

## Data / persistence impact
None. No Room schema or stored-memory changes.

## UI / UX impact
Only responsive composition and spacing. Capture, image import, memory timeline, and persistence semantics remain unchanged.

## Edge cases / regressions
- Very narrow phones must keep capture buttons usable.
- Large screens must not stretch cards edge-to-edge.
- Missing Firebase configuration must not fail Gradle configuration or application startup.
- Existing temporal/OCR behavior must remain unchanged.

## Verification plan
- GitHub Actions `:app:assembleDebug`.
- Review Gradle behavior with no Firebase file.
- Inspect adaptive layout thresholds and constrained widths.
- Confirm TLC files are present in repo.

## Verification results
- AC1 — NOT RUN
- AC2 — CODE REVIEWED; runtime activation requires Firebase config
- AC3 — CODE REVIEWED
- AC4 — CODE REVIEWED
- AC5 — CODE REVIEWED
- AC6 — CODE REVIEWED
- AC7 — PASS
