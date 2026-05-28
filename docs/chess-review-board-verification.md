# Chess Review Board Verification & Integration Notes

Date: 2026-05-28  
Scope: verification/integration review for `.omx/plans/prd-chess-review-board.md` and `.omx/plans/test-spec-chess-review-board.md`.

## Current Review Baseline

This document records the verification lane findings against the current worktree baseline (`4cea41b`) before integration of the dedicated chess review board slices.

## Contract Gaps Found

- Backend analysis is still transient: `POST /chess/analyze` returns `ChessAnalysisResponse` without a persisted `analysisId` draft reference.
- No dedicated chess review domain is present yet: no `ChessAnalysisDraft`, `ChessReview`, `/chess/reviews`, owner-scoped CRUD, draft conversion, match patching, or draft expiry/cleanup code was found.
- Frontend analysis flow still auto-saves the generated prompt to the generic `/boards` endpoint after analysis succeeds.
- The analysis page still lacks the required AI-response textarea and review-create action.
- The app still has only one chess menu route/state; there is no dedicated chess review list/detail route.
- `chess.js` is not listed in `frontend/package.json`, and no matcher fixture script exists.
- Security currently authenticates `POST /chess/analyze`, then broadly permits `/**`; review endpoints need explicit authenticated matchers before any broad permit rule.
- Backend tests cover the existing analysis pipeline but not the draft/review persistence domain, ownership checks, match validation, or dedicated review board behavior.
- Frontend has no configured test runner; review-board UI regressions are currently limited to compile/build checks unless a fixture or test harness is added.

## Integration Checklist

Use this checklist when merging worker slices into the leader branch:

### Backend contract

- `ChessAnalysisResponse` includes a non-empty, unguessable `analysisId` while preserving existing `metadata`, `playerColor`, `moveCount`, `summary`, `moves`, and `aiPrompt` fields.
- Successful analysis persists an owner-bound hidden draft only after Stockfish analysis succeeds.
- Invalid PGN, max-ply violations, Stockfish unavailable, and analysis timeouts do not create drafts.
- `POST /chess/reviews` consumes `analysisId + aiResponse`, creates one complete review, and marks or deletes the draft in the same transaction.
- Review list/detail/update/delete are scoped to the authenticated owner.
- Match edits validate source/confidence enums, unique non-negative `segmentIndex`, and nullable/in-range `matchedPly`; server-derived move fields cannot be corrupted by PATCH payloads.
- Expired, converted, missing, and non-owned drafts return controlled errors without leaking another user's analysis data.
- `/chess/reviews/**` endpoints are authenticated in `SecurityConfig` before any broad permit matcher.

### Frontend contract

- `ChessAnalysisView` no longer calls `/boards` after `/chess/analyze` succeeds.
- After analysis, UI instructs the user to copy the prompt to GPT/Gemini/etc. and paste the external AI response back into the app.
- Review-create button is disabled before analysis and while AI response is blank.
- Dedicated chess review list/detail navigation exists separately from the generic board.
- Review detail restores from saved review data and does not rerun Stockfish.
- `chess.js` is installed and imported only for headless chess state/FEN handling; board rendering remains custom.
- Matcher fixture covers castling SAN, punctuation-stripped SAN, UCI fallback, unmatched/out-of-range references, and multiple references in one segment.

### Verification commands

Run these after integrating implementation slices:

```bash
./gradlew test -x installFrontend -x buildFrontend -x copyFrontend --no-daemon
cd frontend && npm run build
cd frontend && node scripts/check-chess-feedback-matcher.mjs
```

If lint remains configured, run it on the frontend as well:

```bash
cd frontend && npm run lint
```

## Subagent Findings Integrated

- Review probe confirmed the missing backend draft/review domain, stale generic-board auto-save flow, missing dedicated routing, missing `chess.js`/matcher fixture, and security matcher risk.
- Test probe confirmed existing analysis tests are strong but review-domain, ownership, match-validation, and frontend regression coverage are absent.
