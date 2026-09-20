# Falling Blocks Acceptance Evidence

**Shipping status:** NOT ALLOWLISTED  
**Current phase:** Design approved; implementation not started

Statuses are `passed`, `failed`, `blocked-by-environment`, and `not-run`.

| Criterion | Required evidence or artifact | Status | Remaining work |
|---|---|---|---|
| Module boundary | Generated `:game:fallingblocks`, valid dependency report, no forbidden platform/feature/game edges | not-run | Generate only after plan approval; run `validateMiniAppDependencies` |
| Pure deterministic engine | Unit scenarios for actions, invalid actions, collision, spawn, lock and terminal state | not-run | Implement test-first |
| Seven-bag and RNG restore | Bag permutation tests, long-run distribution invariant, snapshot continuation matching uninterrupted run | not-run | Implement test-first |
| SRS rotation | Transition and kick-table tests for every piece/orientation near walls, floor, stack and hidden rows | not-run | Implement corrected downward-positive coordinates |
| No Hold | Source/API inspection and UI semantics contain no Hold state, intent, control, string or tutorial step | not-run | Verify after implementation |
| Gesture-only play | Density-independent tap, horizontal drag, slow downward drag and fast downward fling tests | not-run | Add gesture classifier and Compose integration tests |
| Lock delay | 500 ms boundary and maximum 15 valid reset tests; no instant lock on first blocked descent | not-run | Implement test-first |
| Lines and scoring | Single through four-line, T-spin, combo, back-to-back, perfect-clear, soft/hard-drop and level multiplier tests | not-run | Implement test-first |
| Top-out | Spawn failure and locked hidden-row scenarios display result exactly once | not-run | Implement test-first |
| One endless mode | Manifest/UI inspection shows no difficulty or mode selection | not-run | Verify semantics and screenshots |
| Mandatory tutorial | Fresh install enters four-step practice; no skip affordance; completion persists and starts a clean run | not-run | Unit and Compose tests plus recording |
| Exact resume | Round-trip snapshot resumes board, active piece, queue/bag/RNG, scoring, lock timing and continuation count | not-run | Recreation and relaunch tests |
| Corrupt snapshot recovery | Invalid version, cells, coordinates, bag, counters and durations start clean while preserving best/tutorial | not-run | Add validator tests |
| Game-over presentation | Non-dismissible bottom-sheet-like overlay; board remains sharp, undimmed and visible | not-run | Compact/wide screenshots and dismissal tests |
| Advertisement continuation | Five-second visibility-aware offer, one revive, bottom-four-row recovery, stale/duplicate callback rejection, failure leaves result usable | not-run | Component tests with recording capability |
| New game | Terminal action starts a clean deterministic run and cannot race a pending ad callback | not-run | Component tests |
| Banner eligibility | Session declares `wantsBanner = true`; game does not render or size a banner | not-run | Contract test |
| Centered adaptive field | Board center matches viewport center in compact portrait, wide, tablet and compact-height layouts | not-run | Screenshot/layout-coordinate assertions |
| Theme-only colors | Light/dark and contrasting theme captures; no fixed game color literals or nested theme | not-run | Static inspection and screenshots |
| CRT effect | Static scanlines, restrained glow/bleed, field-only clipping, no jitter; reduced motion retains static scanlines only | not-run | Screenshots and motion capture |
| Motion | Move, rotation, drop, clear/collapse, blocked action and result transitions inspected under normal and reduced motion | not-run | Record representative sessions |
| Accessibility | English labels, non-color piece distinction, predictable result focus, concise board/status semantics and touch behavior | not-run | Compose semantics tests and manual screen-reader pass |
| Shared lock SFX | Block Blast placement thock extracted to `:miniapp:audio-presets`; acoustic regression keeps Block Blast declaration/render unchanged within tolerance | not-run | Add preset and render assertions |
| Original music and SFX | Declarations compile; deterministic render/headroom/budget tests; no supplied melody transcription | not-run | Create and render representative artifacts |
| Audio experience | Listen to loop, intensity changes, repeated movement/drop/lock/clear SFX over music, mute, background/resume and teardown | not-run | Record artifact paths and listening notes |
| Performance | No storage/audio/DSP work in Compose frame callbacks; bounded effects and stable rapid-input behavior on representative devices | not-run | Profile Android and inspect iOS run |
| Android verification | `allTests`, `verifyMiniApp`, Android compilation and relevant host compilation succeed | not-run | Run exact final commands |
| iOS verification | iOS simulator compilation succeeds and live layout/audio are inspected where available | not-run | Run exact final commands and device/simulator review |
| Repository hygiene | `git diff --check`; unrelated worktree files untouched; provenance and limitations documented | not-run | Verify before handoff |
| Shipping gate | `settings.gradle.kts` production allowlist remains unchanged | passed | Reconfirm before handoff |

## Planned Visual Evidence

- Compact portrait: active play, each tutorial gesture, blocked rotation, and
  game-over overlay.
- Wide/landscape: centered field with edge information not shifting its center.
- Tablet: centered field, five-piece preview, score/level/lines, banner-safe host
  layout.
- Light and dark themes, plus reduced-motion captures.
- Motion recording covering hard drop, line sweep/collapse, result entrance,
  app background/resume, and continuation.

## Planned Audio Evidence

- A representative original music loop at base and elevated intensity.
- Individual SFX renders and a mixed stress render with repeated gameplay SFX.
- Determinism hashes or acoustic features, peak/headroom results, mobile render
  budget results, and human listening notes.
- A before/after Block Blast placement render proving the extracted shared
  preset did not unintentionally change its sound.

## Planned Verification Commands

All commands will use the repository-required `rtk` prefix.

```bash
rtk ./gradlew :game:fallingblocks:allTests
rtk ./gradlew :game:fallingblocks:validateMiniAppDependencies
rtk ./gradlew :game:fallingblocks:compileAndroidMain
rtk ./gradlew :game:fallingblocks:compileKotlinIosSimulatorArm64
rtk ./gradlew :game:fallingblocks:verifyMiniApp
rtk ./gradlew :game:blockblast:allTests
rtk ./gradlew :miniapp:audio-presets:allTests
rtk ./gradlew :miniapp:audio-presets:compileAndroidMain
rtk ./gradlew :miniapp:audio-presets:compileKotlinIosSimulatorArm64
rtk ./gradlew :composeApp:compileAndroidMain
rtk git diff --check
```

The exact successful and failed command results, inspected artifacts, device
coverage, unresolved limitations, and final NOT ALLOWLISTED status will replace
this planned evidence during implementation.

