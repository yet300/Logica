# Falling Blocks MiniApp Brief

**Date:** 2026-09-20  
**Phase:** Design approved; implementation plan pending user review  
**Shipping status:** NOT ALLOWLISTED  
**Next decision:** User review of the written design before implementation planning

## Product

- **MiniApp ID:** `game.fallingblocks`
- **Project path:** `:game:fallingblocks`
- **Display name:** Falling Blocks
- **Category:** Game
- **Authors:** Logica contributors, with implementation assistance from OpenAI Codex
- **Mode:** One endless run; no difficulty selector
- **Platforms:** Android and iOS, phone and tablet, portrait and landscape
- **Language at launch:** English only, through Compose Resources

Falling Blocks is a gesture-only falling-tetromino game for Logica. The board
remains the visual center on every window size. The presentation inherits
`MaterialTheme.colorScheme`, adds a restrained theme-derived CRT treatment to
the playfield, and uses original procedural music and effects.

## Approved Player Experience

- No Hold mechanic anywhere in state, controls, UI, persistence, tutorial, or
  audio.
- No gameplay buttons. Tap rotates clockwise, horizontal drag moves, downward
  drag soft-drops, and a fast downward fling hard-drops.
- A mandatory, non-skippable first-run tutorial teaches those four gestures in
  a deterministic practice scene. Completing it starts a fresh run.
- The host toolbar shows current and best score. Level, cleared lines, and five
  upcoming pieces are arranged around the field without moving the field away
  from the viewport center.
- Game over opens a non-dismissible bottom-sheet-like overlay over the live,
  sharp board. There is no blur or dim scrim.
- The result flow offers a five-second `Try Again` advertisement continuation,
  followed by `New Game`. Each run permits one continuation.
- Continuing removes the bottom four rows, shifts the remaining stack down,
  preserves score, level, line count, queue, bag, and random state, resets
  combo/back-to-back/lock state, and spawns a new active piece.
- Exact active-run state resumes automatically after recreation or relaunch.
- The session opts into the host-owned banner using `wantsBanner = true`.

## Approved Rules

- Board: 10 by 20 visible cells plus two hidden spawn rows.
- Pieces: the seven standard free tetrominoes, supplied by a deterministic
  seven-bag with five-piece preview.
- Rotation: clockwise 90-degree rotation with corrected SRS kick tables.
- Horizontal drag is cell-quantized and preserves fractional overshoot so slow
  and fast drags behave consistently across densities.
- A blocked gravity or soft-drop step begins a 500 ms lock delay. Valid movement
  or rotation resets the delay at most 15 times.
- Level rises every ten cleared lines. Gravity uses an integer duration table
  from 800 ms to a floor of 80 ms.
- Top-out occurs when a new piece cannot spawn or a locked piece remains in the
  hidden rows.

Scoring, multiplied by the current level:

| Event | Base points |
|---|---:|
| Single | 100 |
| Double | 300 |
| Triple | 500 |
| Four lines | 800 |
| T-spin, no line | 400 |
| T-spin single | 800 |
| T-spin double | 1200 |
| T-spin triple | 1600 |
| Perfect clear | 2000 |

Back-to-back eligible clears receive a 1.5 multiplier. Combo bonus is
`50 * comboIndex * level`. Soft drop awards one point per cell and hard drop
awards two points per cell.

## Visual and Motion Direction

- Use `core:uikit`, `AdaptiveGameScaffold`, and only semantic roles from the
  inherited `MaterialTheme.colorScheme`; do not introduce a fixed blue or
  tetromino palette and do not nest a custom theme.
- Keep the board geometrically centered in the full gameplay viewport on every
  supported size. Supporting information may occupy edge overlays but may not
  alter the board's center.
- Derive cell differentiation from multiple theme roles, outline, luminance,
  and shape detail so meaning is not color-only.
- Adapt the CRT technique described at
  <https://www.sinasamaki.com/creating-a-crt-screen-effect-in-jetpack-compose/>
  into static scanlines, weak glow, and minimal color bleed on the playfield.
  Do not use constant random jitter. Reduced-motion mode keeps only static
  scanlines.
- Tutorial imagery is inspired by the supplied gesture sequence, but uses
  Material Symbols, an animated touch point, trail, and arrow instead of the
  photographed/3D hand asset.
- Target motion: move 70 ms, rotate 110 ms, hard-drop trail 90 ms plus impact
  140 ms, line sweep 180 ms plus collapse 160 ms, and result panel 280 ms.
  Reduced-motion mode uses immediate state changes or restrained fades.

## Audio Direction

- Compose an original retro-electro/chiptune loop. The supplied Sprudel example
  is mood and layering reference only; its recognizable melody, rhythm, and
  parameters must not be transcribed.
- Target 126 BPM. Game gravity does not change physical music tempo. Intensity
  changes only when level or stack-height band changes.
- Start with bass and percussion, then introduce an original chip lead and
  additional layer at higher intensity.
- Reuse and tune public audio presets before creating game-owned declarations.
- Extract Block Blast's existing wooden placement thock into
  `:miniapp:audio-presets`, keep Block Blast sounding unchanged, and use the
  same shared preset for piece lock in Falling Blocks.
- Other roles remain original: restrained move click, rotate blip, rate-limited
  soft-drop tick, hard-drop impact, distinct line-clear sweeps, four-line and
  perfect-clear success accent, level-up cue, descending game-over cue, and
  revive power-up.
- Playback follows the session-bound `MiniAppAudio` lifecycle. Representative
  loop and SFX-over-music renders require listening review in addition to
  deterministic declaration/render tests.

## Architecture and Persistence

- Generate the module with the repository `game` MiniApp profile and preserve
  its Metro, Decompose, MVIKotlin, resource, and session boundaries.
- A pure common Kotlin engine maps immutable `GameState` and typed `GameAction`
  to new state and typed domain events. Compose owns rendering and input
  translation only.
- Keep the Playing component mounted at game over. A Decompose `childSlot`
  presents the non-dismissible result overlay while gameplay is frozen.
- Result logic owns the visibility-aware countdown and terminal-action gate.
  Advertisement completion is guarded by a unique session/run/action token;
  duplicate or stale callbacks do nothing.
- Persist local versioned keys for `best_score`, `tutorial_seen`, and the active
  game snapshot through `MiniAppStorage` only.
- The snapshot includes board, active piece/position/rotation, preview queue,
  bag and RNG state, score, level, lines, combo/back-to-back state, remaining
  lock duration/reset count, continuation count, and schema version. Durations,
  not wall-clock deadlines, are stored.
- Checkpoint after a piece locks, tutorial completion, continuation, new game,
  top-out, and lifecycle/background transition. Do not write for each dragged
  cell or frame.
- Reject corrupt or unsupported snapshots while preserving independently valid
  best score and tutorial completion.

## Source Evidence and Corrections

The archived TetrisLite repository at <https://github.com/yet300/TetrisLite>
was inspected as behavioral reference. Its repository license file is
Apache-2.0. No source file will be copied verbatim.

The new engine explicitly avoids issues observed in the reference:

- random bag state was not robustly restorable;
- vertical wall-kick signs were questionable for a downward-positive screen
  coordinate system;
- 180-degree rotation was composed from two clockwise rotations;
- gesture thresholds used raw pixels;
- collision at the floor locked immediately instead of using a bounded delay;
- rules and side effects were tightly mixed.

The new game omits both Hold and 180-degree rotation by product decision.

## Scope Exclusions

- Production allowlist modification
- Hold, counter-clockwise rotation, and 180-degree rotation
- Difficulty or mode selection
- Tutorial skip
- Gameplay buttons
- Online leaderboard, multiplayer, replay, daily challenge, or cloud save
- Fixed game palette, imported hand artwork, bundled music, or copied melody
- Direct platform audio, ad SDK, or Settings dependencies

## Acceptance Summary

Implementation is acceptable only when pure-engine behavior, deterministic
restore, stale-safe continuation, adaptive centered layout, theme behavior,
tutorial flow, accessibility, motion, and cross-platform compilation are
verified. Visual and audio quality require inspected artifacts; compilation
alone is insufficient. Detailed evidence is tracked in `acceptance.md`.

