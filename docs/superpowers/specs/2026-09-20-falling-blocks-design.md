# Falling Blocks MiniApp Design

**Date:** 2026-09-20  
**Status:** Approved in conversation; awaiting written-spec review  
**Shipping status:** NOT ALLOWLISTED

## Objective

Create `game.fallingblocks`, an original Kotlin Multiplatform falling-block
MiniApp for Funfolio. It provides one endless, gesture-only mode, exact resumable
state, a mandatory visual tutorial, a centered adaptive board, theme-derived
CRT styling, an advertisement-backed single continuation, and original
procedural audio.

The module follows the same generated MiniApp, Metro, Decompose, MVIKotlin,
storage, audio, resource, and host-frame structure as the repository's other
games. It must not be added to the production allowlist in this change.

## Research and Provenance

The archived <https://github.com/yet300/TetrisLite> project was inspected for
domain behavior. Its license file is Apache-2.0. The new implementation uses
genre-level mechanics and independently authored Kotlin rather than copied
source.

Reference inspection exposed defects or unsuitable choices that must not be
carried forward: non-restorable random progression, questionable vertical kick
signs in downward-positive coordinates, 180-degree rotation composed from two
clockwise operations, raw-pixel gesture thresholds, immediate floor locking,
and side effects mixed with rules.

The supplied tutorial screenshots establish interaction staging and emphasis,
not an asset license. The implementation uses Material Symbols and original
Compose drawing instead of copying the hand artwork. The supplied Sprudel text
establishes a retro layered mood only; its recognizable melody, rhythm, and
parameter sequence are excluded.

The CRT implementation adapts concepts from
<https://www.sinasamaki.com/creating-a-crt-screen-effect-in-jetpack-compose/>:
layer recording, scanline gradients, glow, and restrained color separation. It
does not copy article code verbatim and deliberately omits continuous jitter.

Detailed product decisions and acceptance evidence live in:

- `docs/miniapp/proposals/game.fallingblocks/brief.md`
- `docs/miniapp/proposals/game.fallingblocks/acceptance.md`

## Rules Model

The funfoliol board contains ten columns, twenty visible rows, and two hidden
spawn rows. The standard seven free tetrominoes are produced by a deterministic
seven-bag generator. Five upcoming pieces are visible. There is no Hold state or
action.

An active piece has type, clockwise rotation, integer origin, last successful
rotation metadata for T-spin recognition, and lock-delay state. Rotation uses
the standard SRS transition-specific kick tables expressed and tested in the
engine's downward-positive coordinate system. Only clockwise 90-degree
rotation is exposed; counter-clockwise and 180-degree rotation do not exist.

Gravity uses a named integer duration table beginning at 800 ms and reaching a
floor of 80 ms. Level advances every ten cleared lines. When downward movement
is blocked, gravity and soft drop start a 500 ms lock delay. A successful
horizontal move or rotation resets it at most fifteen times. Hard drop locks
immediately after traversing the last legal distance.

Top-out occurs when the next piece cannot spawn or a locked piece occupies a
hidden row. Line resolution happens after lock, followed by scoring, board
collapse, level update, queue advancement, and spawn. Domain transitions emit
typed facts for presentation and audio without embedding DSP or animation
instructions in the engine.

### Scoring

Normal line clears award 100/300/500/800 points for one through four lines,
multiplied by level. T-spin awards are 400 without a line and 800/1200/1600 for
one through three lines, multiplied by level. Eligible consecutive clears gain
a 1.5 back-to-back multiplier. Combo bonus is
`50 * comboIndex * level`. A perfect clear adds `2000 * level`. Soft and hard
drop award one and two points per descended cell respectively.

All scoring uses integer arithmetic with explicit multiplication ordering and
overflow-safe bounds. Tests define the first combo index and back-to-back
eligibility so presentation cannot reinterpret them.

## Input Contract

The playfield is the sole gameplay input surface:

- a tap requests one clockwise rotation;
- horizontal drag converts accumulated density-independent distance into whole
  cell moves while retaining unused overshoot;
- deliberate downward drag requests rate-limited soft-drop cells;
- a downward fling above a named velocity threshold requests hard drop.

The classifier resolves tap versus drag and slow drag versus fling once per
pointer sequence. It cancels safely on multi-touch, lifecycle interruption, or
loss of visibility. No gameplay button or invisible side-zone control exists.

## Architecture

Generate the module after plan approval:

```bash
./gradlew createMiniApp \
  -PminiAppId=game.fallingblocks \
  -PminiAppName="Falling Blocks" \
  -PminiAppProfile=game
```

`FallingBlocksEngine` is a pure common Kotlin reducer over immutable
`GameState` and typed `GameAction`. It owns generation, legality, movement,
rotation, gravity, locking, line clearing, scoring, top-out, and continuation
recovery. It has no Compose, lifecycle, storage, advertisement, or audio
dependency.

The MVIKotlin Store owns the authoritative session state. Its executor owns the
bounded tick scheduler, translates component intents to engine actions,
serializes checkpoints, and maps domain facts through a semantic audio-player
seam. Reducers remain synchronous and side-effect free. Compose renders models
and translates pointer input; it does not mutate domain state.

The Decompose root retains the Playing component throughout the session and
owns a result `childSlot`. On top-out, ticking and gameplay input freeze while
the live board remains mounted behind a non-dismissible result overlay. The
Result component owns countdown state and mutually exclusive terminal actions.

The Metro session graph consumes `MiniAppSessionContext`, its bound storage and
audio facades, and `MiniAppAdsCapability`. It does not depend on the host,
native ads, platform audio, feature modules, or another game module. The
session declares `wantsBanner = true`; host code retains all banner rendering,
eligibility, sizing, insets, and no-fill behavior.

## Continuation and Advertisement Safety

For five visible seconds after top-out, the result panel offers `Try Again`.
The countdown pauses while the MiniApp is not visible. After expiration, or
after the one continuation is consumed, the action becomes `New Game`.

Requesting continuation creates an opaque game-local fullscreen-ad reason and
a token bound to session identity, run identity, and action ordinal. Only the
matching first completion can revive the run. Failure or cancellation leaves
the result panel usable. Duplicate, stale, post-destruction, and previous-run
callbacks are ignored.

Revival removes the bottom four visible rows, shifts all remaining settled
cells down four rows, preserves score, level, cleared lines, preview queue,
bag, and RNG state, resets combo/back-to-back/current lock state, and spawns the
next piece. If that spawn is impossible, the result state remains terminal.

## Persistence

All values use local snake-case keys through `MiniAppStorage`:

- `best_score`
- `tutorial_seen`
- `game_snapshot`

The versioned snapshot contains settled cells, active piece and origin,
rotation metadata, queue, bag contents and cursor, RNG state, score, level,
lines, combo, back-to-back state, remaining lock duration, reset count,
continuation count, run identity, and schema version. It stores funfoliol
durations rather than wall-clock timestamps and excludes animation, particles,
pointer state, pending audio, and advertisement callbacks.

Checkpoint after lock resolution, tutorial completion, continuation, new game,
top-out, and background/invisibility transition. Use at most one in-flight and
one latest pending checkpoint so slow storage cannot block input or create an
unbounded queue. Persist terminal state before invoking an external close or
replacement transition.

Validation rejects unsupported versions, wrong dimensions, illegal cells,
invalid active pieces or coordinates, malformed bags, inconsistent previews,
negative or impossible counters, and out-of-range durations. Invalid snapshots
are deleted or replaced by a clean run while independently valid best score and
tutorial completion survive.

## Adaptive UI

Use `AdaptiveGameScaffold` in a centered-primary configuration. The board's
geometric center must equal the available gameplay viewport center in compact,
wide, tablet, and compact-height layouts. Five-piece preview and Level/Lines
information may move between edge overlays or compact arrangements, but they
must not participate in the measurement that positions the board.

The host toolbar center content shows current and best score. Host chrome,
Back, Settings, safe areas, banner placement, and system bars remain host-owned.

Every game color derives from the inherited `MaterialTheme.colorScheme`.
Tetromino differentiation uses stable mappings among semantic roles plus
outline/luminance/surface treatment; no fixed blue, cyan, yellow, or private
palette is introduced. The game does not install a nested theme.

The playfield CRT layer uses clipped scanlines, weak bloom, and minimal color
bleed derived from theme roles. It never blurs the underlying terminal board,
does not cover host chrome, and contains no perpetual random displacement.

## Tutorial

When `tutorial_seen` is false, launch a deterministic practice board before the
real run. The tutorial cannot be skipped and teaches, in order:

1. tap to rotate clockwise;
2. drag horizontally to move;
3. drag downward slowly to soft drop;
4. fling downward to hard drop.

Each step advances only after the requested legal gesture is demonstrated.
Material Symbols such as `touch_app`/`swipe`, an animated touch point, trail,
and direction arrow provide instruction. English copy is stored in Compose
Resources. Completing step four persists `tutorial_seen` and starts a fresh
game, ensuring practice pieces and score never leak into the real run.

## Motion and Accessibility

Presentation uses bounded event-driven animation: horizontal movement 70 ms,
rotation 110 ms, hard-drop trail 90 ms with 140 ms impact, line sweep 180 ms
with 160 ms collapse, and result entrance 280 ms. An invalid rotation produces
a short outline pulse without moving the piece. No cell owns a coroutine or
independent long-lived animator.

Reduced-motion mode removes translation flourishes, trails, bloom expansion,
and collapse motion, retaining immediate state updates or short opacity fades
and static scanlines.

Semantics expose score, best score, level, lines, next pieces, tutorial
instruction, paused/result state, and available result action. Game state does
not rely on color alone. Focus moves predictably to the result action and cannot
escape into a dismiss gesture. The field provides concise aggregate semantics
instead of creating a focus target for every cell.

## Audio

Music is an original 126 BPM retro-electro/chiptune declaration built through
the public procedural-audio API. It uses a chip lead, analog-style bass,
game-owned pulse/noise percussion where presets are insufficient, and an
original motif unrelated to the supplied recognizable tune. The base layer
starts with bass and percussion. Higher level or stack-height bands add lead or
density; gravity speed never changes transport tempo.

SFX roles are typed and semantic: movement click, rotation blip, rate-limited
soft-drop tick, hard-drop impact, piece lock, one-to-four-line clears, perfect
clear, level up, game over, and revive. Movement repetition is bounded so a
drag cannot flood commands.

Piece lock reuses Block Blast's existing wooden thock. Its declaration moves
to `:miniapp:audio-presets`; Block Blast is updated to reference the shared
preset without an intentional acoustic change. Acoustic render assertions
guard the extraction. Other Falling Blocks sounds are original or tuned public
presets following the repository reuse order.

Audio playback is session-bound and respects host visibility, settings,
fullscreen advertisement suppression, backgrounding, and teardown. Rejected
commands are dropped or logged through the existing policy without retry loops
or gameplay impact.

## Failure Handling

- Illegal actions and gestures with unmet preconditions are ignored without
  partial mutation.
- Excess tick lag is bounded and cannot cause an unbounded catch-up loop.
- Storage failure leaves the in-memory run playable and retries only at the
  next meaningful checkpoint; terminal state remains on screen.
- Snapshot corruption starts clean while preserving independent preferences.
- Audio rejection never blocks the Store.
- Advertisement failure keeps the result action available; stale callbacks are
  idempotently ignored.
- Session destruction stops ticks, cancels jobs, performs a final bounded
  checkpoint, closes audio through host ownership, and cannot mutate afterward.

## Test Strategy

Implementation follows red-green-refactor. Pure tests cover piece geometry,
every SRS transition/kick context, bag permutation and restoration, movement,
gravity, lock delay/reset cap, top-out, line collapse, scoring, T-spins,
back-to-back, combos, perfect clears, level speed, continuation, and state
validation.

Component tests cover tick visibility, checkpoint coalescing, exact recreation,
tutorial persistence, countdown pausing, advertisement failure, duplicate/stale
callbacks, one-continuation enforcement, new-game races, and teardown.

Compose tests cover gesture classification across densities, mandatory tutorial
semantics, centered-board coordinates across window classes, edge information,
theme inheritance, game-over non-dismissal, absence of blur/dim, accessibility,
and reduced motion.

Audio tests compile every declaration and assert deterministic representative
renders, frequency/energy distinctions, peak headroom, mobile work budgets, and
Block Blast lock-sound regression. Human review listens to the loop, intensity
transitions, repeated SFX over music, mute/background/resume, advertisement
suppression, and destruction.

## Verification

The implementation plan will use focused tasks first, then the affected slice:

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

Visual, motion, audio, and live lifecycle evidence remain separate from passing
automated checks. Final handoff records exact outcomes and unresolved device
coverage in the acceptance file.

## Known Limitations and Exclusions

- Initial UI copy is English only.
- There is one endless mode and no difficulty selection.
- There is no Hold, counter-clockwise rotation, 180-degree rotation, replay,
  online leaderboard, multiplayer, daily challenge, or cloud synchronization.
- The tutorial cannot be skipped.
- No custom product icon is included in the initial contributor change.
- The module remains NOT ALLOWLISTED until independent maintainer review and a
  separate production shipping decision.

