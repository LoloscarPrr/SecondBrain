# SecondBrain agent instructions

All repository changes must follow the local skill at `.agents/skills/tlc-spec-driven/SKILL.md`.

## Required workflow
1. Initialize repository/product context when starting a new session or when context may be stale.
2. Think through the affected behavior, persistence, UI, integrations, and regressions.
3. Lock a spec under `docs/specs/` before implementation.
4. Implement the smallest coherent change.
5. Verify every acceptance criterion and record PASS/BLOCKED evidence.

## Product rules
- Preserve local-first memory behavior unless a locked spec explicitly changes it.
- Never silently discard raw captures or source provenance.
- Temporal interpretation changes require regression examples.
- Image/OCR changes must verify that totals, line-items, dates, and unrelated numbers are not conflated.
- Adaptive UI changes must check compact, normal, and large layouts.
- Crash reporting must not prevent local development or CI when Firebase configuration is unavailable.
- Do not claim a build/test passed unless the corresponding check was actually observed.

## Spec IDs
Use `SB-<AREA>-NNN`, for example `SB-CORE-001`, `SB-MEM-004`, `SB-UI-002`, `SB-IMG-003`.
