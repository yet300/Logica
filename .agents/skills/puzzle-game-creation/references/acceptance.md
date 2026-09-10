# Acceptance evidence

Maintain `docs/miniapp/proposals/<game-id>/acceptance.md` with criterion, evidence/artifact, status and remaining work. Statuses: passed, failed, blocked-by-environment, not-run. Keep aesthetic approval distinct from automated correctness.

## Rules and levels

- Pure engine scenarios cover valid/invalid input, win/fail and recovery; persistence resumes the same puzzle and progress.
- Define a level schema, stable ID/version, reproducible generator/seed if used, solution validation and difficulty measures specific to the mechanic (dependency depth, branching, forced moves, decision count).
- Validate every finite shipped level where feasible; otherwise state exactly which corpus/sample was checked and why. A bounded seeded sample of an endless generator does not prove all possible outputs valid.
- For generated puzzles, use constructive solvability or an independent solver/checker appropriate to the rules. Avoid validating solely by replaying the same faulty generation logic.
- Check exact and meaningful structural duplicates, including rotations/reflections when equivalent in this game. Count validated distinct puzzles separately from seeds, templates, or promised future content.
- Review representative early/middle/late puzzles and transitions in difficulty through play. Bigger boards alone do not prove difficulty or variety. Never silently substitute a demo pack for the agreed count.

## Playable experience

- Capture and inspect compact/wide gameplay, tutorial, blocked action and completion. Compare to approved samples. Check host chrome, banners, accessibility, zoom/hit-testing if applicable, text overflow and rapid input.
- Record and inspect motion, interruptions, next-level flow and reduced motion. A passing screenshot check is not motion verification.
- Provide the audio palette, original preview files, declaration tests and listening notes. Check loops, repeated SFX over music, mute/background/resume and session destruction. Explicitly record user-approved silence if applicable.
- Review icon/font source records and confirm no invented interface assets or unapproved generated product icon remain.

## Repository verification

Follow the current contributor protocol and affected module tasks rather than assuming targets. Run the narrow rules/declaration tests, `./gradlew :game:<name>:verifyMiniApp` and `git diff --check` (use the repository's command wrapper). Record exact commands/results. Device checks and actual listening remain separate evidence; report unavailable platforms. Verify the actual built slice before scaling and the final corpus afterward. Do not rerun unrelated suites without a reason.

Finish with delivered versus agreed scope, validated level count, screenshots/recordings/audio, exact verification results, unresolved limitations and shipping status. Do not use a numerical aesthetic score to hide a failed essential requirement.
