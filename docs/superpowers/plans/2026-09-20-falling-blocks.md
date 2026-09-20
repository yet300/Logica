# Falling Blocks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the non-allowlisted `game.fallingblocks` KMP MiniApp with deterministic modern falling-block rules, gesture-only play, exact resume, mandatory tutorial, resilient game-over continuation, centered theme-derived CRT UI, and original procedural audio.

**Architecture:** A pure immutable common Kotlin engine owns every game rule and emits typed facts. An MVIKotlin Store owns ticking, persistence and audio coordination; a retained Decompose root keeps Playing mounted while a `childSlot` presents the result overlay. Compose translates gestures and renders state only, while Metro supplies session-bound storage, ads and audio contracts.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform/Material 3, Decompose, MVIKotlin, Metro, kotlinx.serialization, Compose Resources, `core:uikit`, MiniApp storage/ads/audio APIs, kotlin-test.

---

## Preconditions and Execution Rules

- Work on branch `codex/fallingblocks` from repository root.
- Preserve the unrelated untracked `iosApp/KotlinMultiplatformLinkedPackage/subpackages/dev_gitlive_firebase_*` directories.
- Prefix every shell command with `rtk`.
- Before Task 1, read the complete skills used by the implementation:
  `superpowers:test-driven-development`, `decompose-component`,
  `decompose-navigation`, `decompose-compose`, `mvikotlin-code`, `metro-di`,
  `compose-state-authoring`, `compose-state-hoisting`,
  `compose-modifier-and-layout-style`, `compose-multiplatform-adaptive-design`,
  `compose-animations`, `compose-ui-testing-patterns`,
  `miniapp-procedural-audio`, and `superpowers:verification-before-completion`.
- Keep `settings.gradle.kts` production `miniApps` allowlist unchanged.
- For every task: write the named failing test first, run the narrow test and
  observe the expected failure, implement only the described slice, rerun the
  test, then commit only the listed files.

## Locked File Structure

The generator creates the module shell. Refactor its placeholder root/state
files into these focused responsibilities:

```text
game/fallingblocks/
├── build.gradle.kts
├── AGENTS.md
├── PROVENANCE.md
└── src/
    ├── commonMain/
    │   ├── composeResources/{values/strings.xml,drawable/miniapp_icon.xml}
    │   └── kotlin/ge/yet/game/fallingblocks/
    │       ├── FallingblocksPlugin.kt
    │       ├── FallingblocksSession.kt
    │       ├── audio/{FallingBlocksAudio.kt,FallingBlocksAudioAdapter.kt,FallingBlocksProgram.kt}
    │       ├── component/game/{FallingBlocksComponent.kt,DefaultFallingBlocksComponent.kt}
    │       ├── component/game/store/{FallingBlocksStore.kt,FallingBlocksStoreFactory.kt,TickPlanner.kt}
    │       ├── component/result/{ResultComponent.kt,DefaultResultComponent.kt}
    │       ├── component/root/{RootComponent.kt,DefaultRootComponent.kt}
    │       ├── data/{FallingBlocksPersistence.kt,FallingBlocksSchemas.kt,SessionPersistenceCoordinator.kt}
    │       ├── di/{FallingblocksSessionBindings.kt,FallingblocksSessionGraph.kt}
    │       ├── domain/engine/{FallingBlocksEngine.kt,RandomState.kt,SevenBag.kt,SrsRotation.kt,Scoring.kt}
    │       ├── domain/model/{Board.kt,Tetromino.kt,ActivePiece.kt,FallingBlocksState.kt,GameTransition.kt}
    │       ├── domain/repository/{GameSnapshotLoader.kt,GameCommitWriter.kt,TutorialSeenRepository.kt}
    │       └── ui/
    │           ├── board/{BoardGeometry.kt,CrtBoard.kt,TetrominoStyle.kt}
    │           ├── input/{GestureClassifier.kt,FallingBlocksGestures.kt}
    │           ├── motion/FallingBlocksMotionPolicy.kt
    │           ├── result/ResultOverlay.kt
    │           ├── screen/{FallingBlocksScreen.kt,root/RootContent.kt,root/RootTopBarContent.kt}
    │           └── tutorial/{TutorialOverlay.kt,TutorialStep.kt}
    └── commonTest/kotlin/ge/yet/game/fallingblocks/
        ├── TestFallingBlocksFixtures.kt
        └── matching engine, persistence, component, UI and audio test packages
```

The generator derives `Fallingblocks` from the ID segment, so keep that spelling
for generated public plugin/session/graph names. Use `FallingBlocks` for the
game-owned domain, Store, component, UI and audio types.

### Task 1: Generate and verify the non-shipping module shell

**Files:**
- Create through generator: `game/fallingblocks/**`
- Do not modify: `settings.gradle.kts`
- Modify after generation: `game/fallingblocks/src/commonMain/composeResources/values/strings.xml`
- Create: `game/fallingblocks/PROVENANCE.md`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/FallingblocksPluginContractTest.kt`

- [ ] **Step 1: Generate the standard game profile**

Run:

```bash
rtk ./gradlew createMiniApp -PminiAppId=game.fallingblocks -PminiAppName="Falling Blocks" -PminiAppProfile=game
```

Expected: `:game:fallingblocks` is created and discovery reports that it is not
in the shipping allowlist.

- [ ] **Step 2: Replace placeholder strings with the English contract**

Use this exact resource surface; later UI tasks may add strings but must not add
hard-coded user text:

```xml
<resources>
    <string name="miniapp_title">Falling Blocks</string>
    <string name="miniapp_description">Stack falling pieces and clear lines.</string>
    <string name="score_label">Score</string>
    <string name="best_label">Best</string>
    <string name="level_label">Level</string>
    <string name="lines_label">Lines</string>
    <string name="next_label">Next</string>
    <string name="tutorial_tap">Tap the field to rotate</string>
    <string name="tutorial_move">Drag sideways to move</string>
    <string name="tutorial_soft_drop">Drag down slowly to soft drop</string>
    <string name="tutorial_hard_drop">Fling down to hard drop</string>
    <string name="game_over_title">Game over</string>
    <string name="try_again_action">Try Again</string>
    <string name="new_game_action">New Game</string>
    <string name="advertisement_suffix">Advertisement</string>
</resources>
```

- [ ] **Step 3: Strengthen the generated plugin contract**

Add assertions to `FallingblocksPluginContractTest`:

```kotlin
assertEquals(MiniAppId("game.fallingblocks"), plugin.manifest.id)
val session = plugin.createSession(harness.context)
assertTrue(session.wantsBanner)
MiniAppContractAssertions.assertRetainedGraphSession(session)
```

Run:

```bash
rtk ./gradlew :game:fallingblocks:allTests :game:fallingblocks:validateMiniAppDependencies
```

Expected before the session edit: FAIL because `wantsBanner` is false. Change
`FallingblocksSession` to extend `DelegatingMiniAppSession(wantsBanner = true)`;
then rerun and expect PASS.

- [ ] **Step 4: Record provenance and shipping state**

Create `PROVENANCE.md` with: TetrisLite URL and Apache-2.0 classification,
explicit independent implementation, CRT article URL, supplied screenshots and
Sprudel text as non-copied references, original Compose visuals/procedural audio,
Codex assistance, known English-only limitation, and `NOT ALLOWLISTED`.

- [ ] **Step 5: Commit the shell**

```bash
rtk git add game/fallingblocks
rtk git commit -m "feat: scaffold falling blocks miniapp"
```

### Task 2: Extract the unchanged wooden lock SFX into audio presets

**Files:**
- Modify: `miniapp/audio-presets/src/commonMain/kotlin/ge/yet/game/miniapp/audio/presets/SoundEffects.kt`
- Modify: `miniapp/audio-presets/src/commonTest/kotlin/ge/yet/game/miniapp/audio/presets/SoundEffectsTest.kt`
- Modify: `game/blockblast/src/commonMain/kotlin/ge/yet/game/blockblast/audio/BlockBlastSfx.kt`
- Modify: `game/blockblast/src/commonMain/kotlin/ge/yet/game/blockblast/audio/BlockBlastAudio.kt`
- Modify: `game/blockblast/src/commonTest/kotlin/ge/yet/game/blockblast/audio/BlockBlastAudioRenderTest.kt`

- [ ] **Step 1: Freeze the current Block Blast acoustic signature**

Add a render assertion for `BlockBlastAudio.Place` over the same fixed window
used by neighboring tests. Assert peak, RMS, centroid and duration tolerances
around the current render rather than PCM bit equality:

```kotlin
val render = renderBlockBlastSfx(BlockBlastAudio.Place)
assertTrue(render.peak in PLACE_PEAK_RANGE)
assertTrue(render.rms in PLACE_RMS_RANGE)
assertTrue(render.spectralCentroidHz in PLACE_CENTROID_RANGE)
assertEquals(PLACE_FRAME_COUNT, render.frameCount)
```

Run `rtk ./gradlew :game:blockblast:allTests` and record the passing baseline.

- [ ] **Step 2: Add the reusable preset declaration**

Add to `SoundEffects.kt`:

```kotlin
fun WoodenPlacementThock(
    name: String = "wooden_placement_thock",
    gain: Float = 1f,
): AudioProgramFragment {
    val level = gain.requirePresetGain()
    return audioProgramFragment {
        sfx(name) {
            oscillator(OscillatorShape.SINE, gain = 0.22f * level)
            noise(NoiseColor.PINK, gain = 0.06f * level, seed = 8_048_402L)
            pitch(from = 170.hz, to = 85.hz, duration = 70.ms)
            envelope(attack = 1.ms, decay = 40.ms, sustain = 0.10f, release = 60.ms)
            lowPass(cutoff = 900.hz, resonance = 0.08f)
        }
    }
}
```

Use the exact public factory/building types already used in `PlacementClick`;
the resulting declaration values must be identical to the current Block Blast
fragment.

- [ ] **Step 3: Test the preset controls and determinism**

```kotlin
@Test
fun `wooden placement thock is deterministic and bounded`() {
    val fragment = WoodenPlacementThock(name = "lock")
    val first = render(fragment)
    val second = render(fragment)
    assertEquals(first.quantizedPcmHash, second.quantizedPcmHash)
    assertContentEquals(first.left, second.left)
    assertContentEquals(first.right, second.right)
    assertTrue(first.peak < 1f)
}
```

Run `rtk ./gradlew :miniapp:audio-presets:allTests`; expect PASS.

- [ ] **Step 4: Compose Block Blast from the shared preset**

Remove only the inline `Place` declaration and include
`WoodenPlacementThock(name = BlockBlastAudio.Place.value)` in
`BlockBlastAudio.program`. Run:

```bash
rtk ./gradlew :game:blockblast:allTests :miniapp:audio-presets:allTests
```

Expected: all tests pass and the frozen acoustic ranges remain unchanged.

- [ ] **Step 5: Commit the shared preset extraction**

```bash
rtk git add miniapp/audio-presets game/blockblast/src/commonMain/kotlin/ge/yet/game/blockblast/audio game/blockblast/src/commonTest/kotlin/ge/yet/game/blockblast/audio
rtk git commit -m "refactor: share wooden placement sound"
```

### Task 3: Define immutable board, piece and deterministic bag models

**Files:**
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/model/Board.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/model/Tetromino.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/model/ActivePiece.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/model/FallingBlocksState.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/model/GameTransition.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/engine/RandomState.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/engine/SevenBag.kt`
- Create: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/TestFallingBlocksFixtures.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/engine/BoardTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/engine/RandomStateTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/engine/SevenBagTest.kt`
- Delete after migration: generated `FallingblocksGameState.kt`

- [ ] **Step 1: Write value-object and invariant tests**

```kotlin
@Test fun `board has ten columns twenty visible and two hidden rows`() {
    assertEquals(10, Board.WIDTH)
    assertEquals(22, Board.TOTAL_HEIGHT)
    assertEquals(2, Board.HIDDEN_ROWS)
    assertFailsWith<IllegalArgumentException> { Board(List(219) { null }) }
}

@Test fun `each bag contains each tetromino exactly once`() {
    val draw = SevenBag.initial(RandomState(42)).draw(7)
    assertEquals(Tetromino.entries.toSet(), draw.items.toSet())
}

@Test fun `restored random and bag state continue identically`() {
    val original = SevenBag.initial(RandomState(91)).draw(19).next
    assertEquals(original.draw(30), original.copy().draw(30))
}
```

Run the specific tests; expect compilation failure because types do not exist.

- [ ] **Step 2: Implement compact immutable models**

Use these stable signatures:

```kotlin
enum class Tetromino { I, J, L, O, S, T, Z }
enum class Rotation { SPAWN, RIGHT, REVERSE, LEFT }
data class Cell(val x: Int, val y: Int)
data class ActivePiece(val type: Tetromino, val rotation: Rotation, val origin: Cell)
data class Board(val cells: List<Tetromino?>) {
    companion object { const val WIDTH = 10; const val VISIBLE_HEIGHT = 20; const val HIDDEN_ROWS = 2; const val TOTAL_HEIGHT = 22 }
}
@JvmInline value class RandomState(val bits: Long)
data class BagState(val remaining: List<Tetromino>, val random: RandomState)
```

`RandomState.nextInt(bound)` returns the value and next state without using
platform random APIs. `SevenBag.draw(count)` performs deterministic Fisher-Yates
refills and returns `BagDraw(items, next)`.

- [ ] **Step 3: Define state, actions and typed facts**

```kotlin
data class FallingBlocksState(
    val board: Board,
    val active: ActivePiece,
    val preview: List<Tetromino>,
    val bag: BagState,
    val score: Long,
    val level: Int,
    val lines: Int,
    val combo: Int,
    val backToBack: Boolean,
    val lockRemainingMillis: Int,
    val lockResetCount: Int,
    val revivesUsed: Int,
    val runId: Long,
    val phase: GamePhase,
)

sealed interface GameAction {
    data object RotateClockwise : GameAction
    data class MoveHorizontal(val cells: Int) : GameAction
    data class SoftDrop(val cells: Int) : GameAction
    data object HardDrop : GameAction
    data class AdvanceTime(val millis: Int) : GameAction
    data object Revive : GameAction
}

data class GameTransition(val state: FallingBlocksState, val facts: List<GameFact>)
enum class GamePhase { PLAYING, TERMINAL }
sealed interface GameFact {
    data class Moved(val horizontalCells: Int, val downwardCells: Int) : GameFact
    data object Rotated : GameFact
    data object Blocked : GameFact
    data class HardDropped(val cells: Int) : GameFact
    data object Locked : GameFact
    data class LinesCleared(val count: Int, val perfect: Boolean) : GameFact
    data class LevelChanged(val level: Int) : GameFact
    data object ToppedOut : GameFact
    data object Revived : GameFact
}

interface FallingBlocksEngine {
    fun initial(seed: Long, runId: Long): FallingBlocksState
    fun reduce(state: FallingBlocksState, action: GameAction): GameTransition
}
```

Define facts for moved, rotated, blocked, soft/hard drop, locked, lines cleared,
level changed, perfect clear, topped out and revived. There is no Hold fact or
action.

Create reusable test builders with explicit defaults:

```kotlin
internal fun gameFixture(
    board: Board = Board.empty(),
    active: ActivePiece = ActivePiece(Tetromino.T, Rotation.SPAWN, Cell(3, 2)),
    score: Long = 0,
    level: Int = 1,
    lines: Int = 0,
    combo: Int = -1,
    backToBack: Boolean = false,
    lockRemainingMillis: Int = 500,
    lockResetCount: Int = 0,
    revivesUsed: Int = 0,
    phase: GamePhase = GamePhase.PLAYING,
): FallingBlocksState

internal fun boardWith(vararg occupied: Pair<Cell, Tetromino>): Board
internal fun restingFixture(lockRemainingMillis: Int = 500, lockResetCount: Int = 0): FallingBlocksState
internal fun terminalFixture(): FallingBlocksState
internal fun spawnBlockedFixture(): FallingBlocksState
internal fun hiddenRowLockFixture(): FallingBlocksState
```

- [ ] **Step 4: Run model and bag tests**

```bash
rtk ./gradlew :game:fallingblocks:allTests --tests '*BoardTest' --tests '*SevenBagTest' --tests '*RandomStateTest'
```

Expected: PASS.

- [ ] **Step 5: Commit the domain foundation**

```bash
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/engine
rtk git commit -m "feat: add deterministic falling blocks model"
```

### Task 4: Implement piece geometry and corrected clockwise SRS

**Files:**
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/engine/SrsRotation.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/engine/FallingBlocksEngine.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/engine/SrsRotationTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/engine/FallingBlocksMovementTest.kt`
- Delete after migration: generated `FallingblocksGameEngine.kt` and test

- [ ] **Step 1: Write table-driven geometry tests**

For every piece and rotation assert four distinct local cells, O invariance,
I pivot behavior, wall/floor rejection, and stack collision. Include explicit
clockwise transition fixtures for spawn→right, right→reverse,
reverse→left and left→spawn.

```kotlin
@Test fun `clockwise T rotation kicks away from left wall`() {
    val state = fixture(active = ActivePiece(Tetromino.T, Rotation.SPAWN, Cell(0, 6)))
    val transition = engine.reduce(state, GameAction.RotateClockwise)
    assertEquals(Rotation.RIGHT, transition.state.active.rotation)
    assertTrue(transition.state.active.cells().all(board::contains))
}
```

- [ ] **Step 2: Implement geometry and transition-specific kick tables**

Expose only:

```kotlin
internal fun ActivePiece.cells(): List<Cell>
internal fun tryRotateClockwise(piece: ActivePiece, board: Board): ActivePiece?
internal fun canOccupy(piece: ActivePiece, board: Board): Boolean
```

Encode JLSTZ and I kick tables separately. Convert canonical upward-positive
SRS offsets once at declaration into this engine's downward-positive Y; do not
negate offsets opportunistically inside collision code.

- [ ] **Step 3: Implement horizontal moves as repeated legal cell steps**

`MoveHorizontal(n)` applies `abs(n)` single-cell attempts, stops at the first
collision, emits one `Moved` fact with actual distance, and leaves state
unchanged when actual distance is zero.

- [ ] **Step 4: Run the focused engine tests**

```bash
rtk ./gradlew :game:fallingblocks:allTests --tests '*SrsRotationTest' --tests '*FallingBlocksMovementTest'
```

Expected: PASS for every table row and boundary fixture.

- [ ] **Step 5: Commit geometry and rotation**

```bash
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/engine game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/engine
rtk git commit -m "feat: implement falling piece movement and srs"
```

### Task 5: Implement gravity, lock delay, clears and scoring

**Files:**
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/engine/Scoring.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/engine/FallingBlocksEngine.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/model/FallingBlocksState.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/engine/LockDelayTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/engine/LineClearTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/engine/ScoringTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/engine/TSpinTest.kt`

- [ ] **Step 1: Write failing lock and timing tests**

```kotlin
@Test fun `first blocked gravity step starts delay without locking`() {
    val result = engine.reduce(restingFixture(), GameAction.AdvanceTime(499))
    assertFalse(result.facts.any { it is GameFact.Locked })
    assertEquals(1, result.state.lockRemainingMillis)
}

@Test fun `valid movement resets lock delay no more than fifteen times`() {
    val state = restingFixture(lockResetCount = 15, lockRemainingMillis = 1)
    val moved = engine.reduce(state, GameAction.MoveHorizontal(1)).state
    assertEquals(1, moved.lockRemainingMillis)
    assertEquals(15, moved.lockResetCount)
}
```

- [ ] **Step 2: Add the integer gravity table and bounded tick consumption**

```kotlin
internal val GRAVITY_MILLIS = intArrayOf(
    800, 720, 630, 550, 470, 400, 340, 290, 240, 200,
    170, 145, 125, 110, 100, 90, 85, 80,
)
internal const val LOCK_DELAY_MILLIS = 500
internal const val MAX_LOCK_RESETS = 15
```

`AdvanceTime` consumes elapsed duration using integer remainders and a named
maximum step budget; it never loops without a hard bound.

- [ ] **Step 3: Write scoring/T-spin/line-collapse tests**

Cover line counts zero through four, T-spin corner recognition with last-action
rotation, B2B start/continue/break, combo start/reset, perfect clear, level
boundary, soft/hard drop distances and checked Long arithmetic.

```kotlin
assertEquals(800L * level, score(ClearKind.FOUR, level).linePoints)
assertEquals(1_600L * level, score(ClearKind.T_SPIN_TRIPLE, level).linePoints)
assertEquals(2_000L * level, score(ClearKind.PERFECT, level).perfectClearPoints)
```

- [ ] **Step 4: Implement lock resolution and spawn ordering**

Order is: merge active cells → detect T-spin → find full visible rows → score →
collapse all rows including hidden rows → update level → advance preview/bag →
spawn → top-out check. Run:

```bash
rtk ./gradlew :game:fallingblocks:allTests --tests '*LockDelayTest' --tests '*LineClearTest' --tests '*ScoringTest' --tests '*TSpinTest'
```

Expected: PASS.

- [ ] **Step 5: Commit timing and scoring**

```bash
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/engine
rtk git commit -m "feat: add falling blocks timing and scoring"
```

### Task 6: Add top-out, revive and generated invariant coverage

**Files:**
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/engine/FallingBlocksEngine.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/engine/TerminalAndReviveTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/engine/FallingBlocksPropertyTest.kt`

- [ ] **Step 1: Write terminal and revive tests**

```kotlin
@Test fun `spawn collision tops out exactly once`() {
    val first = engine.reduce(spawnBlockedFixture(), GameAction.HardDrop)
    val second = engine.reduce(first.state, GameAction.AdvanceTime(1_000))
    assertEquals(GamePhase.TERMINAL, first.state.phase)
    assertEquals(1, first.facts.count { it is GameFact.ToppedOut })
    assertEquals(first.state, second.state)
    assertTrue(second.facts.isEmpty())
}

@Test fun `locked hidden cell tops out`() {
    val result = engine.reduce(hiddenRowLockFixture(), GameAction.HardDrop)
    assertEquals(GamePhase.TERMINAL, result.state.phase)
    assertTrue(result.facts.any { it is GameFact.ToppedOut })
}
@Test fun `revive removes bottom four rows and preserves progression`() {
    val revived = engine.reduce(terminalFixture(), GameAction.Revive).state
    assertEquals(terminal.score, revived.score)
    assertEquals(terminal.level, revived.level)
    assertEquals(terminal.preview, revived.preview)
    assertEquals(1, revived.revivesUsed)
    assertFalse(revived.backToBack)
    assertEquals(-1, revived.combo)
}
```

- [ ] **Step 2: Implement the exact revive transform**

Clear visible Y rows `TOTAL_HEIGHT - 4 until TOTAL_HEIGHT`, shift every retained
occupied cell down four, reset combo/B2B/lock metadata, preserve bag/preview and
progression, then spawn from the preserved queue. Reject revive unless phase is
terminal and `revivesUsed == 0`.

- [ ] **Step 3: Add deterministic generated action sequences**

For seeds 0..255 and 2,000 bounded legal/illegal actions per seed, assert board
size, cell bounds, no active/settled overlap, five previews, bag validity,
non-negative score/lines, bounded lock duration/reset count, and identical
replay for equal seed/actions.

- [ ] **Step 4: Run the complete engine suite**

```bash
rtk ./gradlew :game:fallingblocks:allTests --tests '*engine*'
```

Expected: PASS without hangs or platform random dependencies.

- [ ] **Step 5: Commit terminal rules**

```bash
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/engine
rtk git commit -m "feat: add falling blocks terminal recovery"
```

### Task 7: Add versioned exact persistence and coalesced checkpoints

**Files:**
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/repository/GameSnapshotLoader.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/repository/GameCommitWriter.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/repository/TutorialSeenRepository.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/data/FallingBlocksSchemas.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/data/FallingBlocksPersistence.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/data/SessionPersistenceCoordinator.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/persistence/FallingBlocksSchemasTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/persistence/FallingBlocksPersistenceTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/persistence/SessionPersistenceCoordinatorTest.kt`

- [ ] **Step 1: Write round-trip and corruption tests**

Use local keys exactly `best_score`, `tutorial_seen`, `game_snapshot`. Assert a
snapshot round trip preserves every authoritative field. Reject version,
dimension, enum, cell-count, bag permutation, preview length, active overlap,
negative counters, lock duration and revive count corruption while retaining
best/tutorial values.

- [ ] **Step 2: Define the schema boundary**

```kotlin
@Serializable
internal data class GameSnapshotV1(
    val version: Int = 1,
    val cells: List<Int>,
    val activeType: Int,
    val activeRotation: Int,
    val activeX: Int,
    val activeY: Int,
    val preview: List<Int>,
    val bag: List<Int>,
    val randomBits: Long,
    val score: Long,
    val level: Int,
    val lines: Int,
    val combo: Int,
    val backToBack: Boolean,
    val lockRemainingMillis: Int,
    val lockResetCount: Int,
    val revivesUsed: Int,
    val runId: Long,
    val phase: Int,
)
```

Keep schema DTOs separate from domain models. Encode empty cells as `-1` and
piece ordinals as stable schema mappings, not raw `enum.ordinal` calls.

- [ ] **Step 3: Implement storage adapters**

Use only `MiniAppStorage.getLong/putLong`, `getBoolean/putBoolean`,
`readSnapshot/writeSnapshot/remove`. Propagate coroutine cancellation; return
typed load/write failure for other exceptions.

- [ ] **Step 4: Implement one-in-flight plus latest-pending coordination**

```kotlin
internal interface SessionPersistenceCoordinator {
    suspend fun load(): RestoredSession
    fun checkpoint(state: FallingBlocksState)
    suspend fun flush(state: FallingBlocksState): CommitResult
}
```

Tests inject a suspended first write, enqueue multiple states and assert only
the first plus latest state are written and terminal `flush` establishes an
exact barrier.

- [ ] **Step 5: Run and commit persistence**

```bash
rtk ./gradlew :game:fallingblocks:allTests --tests '*Persistence*' --tests '*SchemasTest'
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/data game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/repository game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/persistence
rtk git commit -m "feat: persist exact falling blocks sessions"
```

### Task 8: Build the MVIKotlin Store and visibility-aware tick loop

**Files:**
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/store/FallingBlocksStore.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/store/FallingBlocksStoreFactory.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/store/TickPlanner.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/FallingBlocksComponent.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/DefaultFallingBlocksComponent.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/component/game/store/TickPlannerTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/component/game/store/FallingBlocksStoreTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/component/game/store/FallingBlocksStorePersistenceTest.kt`

- [ ] **Step 1: Define Store contracts and failing bootstrap tests**

```kotlin
internal interface FallingBlocksStore : Store<Intent, State, Label> {
    sealed interface Intent {
        data object Rotate : Intent
        data class Move(val cells: Int) : Intent
        data class SoftDrop(val cells: Int) : Intent
        data object HardDrop : Intent
        data class Frame(val elapsedMillis: Int) : Intent
        data object Revive : Intent
        data object NewGame : Intent
    }
    data class State(val game: FallingBlocksState?, val loading: Boolean, val tutorialSeen: Boolean)
    sealed interface Label { data class ToppedOut(val runId: Long) : Label }
}
```

Test fresh bootstrap, exact restored bootstrap, terminal restore, corrupt-load
fallback, and no input before bootstrap completes.

- [ ] **Step 2: Add a bounded tick planner**

`TickPlanner.consume(elapsedMillis)` clamps a single frame gap, preserves an
integer remainder and emits at most three engine advances per rendered frame.
Visibility outside `ACTIVE` stops scheduling and triggers one checkpoint.

- [ ] **Step 3: Map engine facts to state, persistence and labels**

Checkpoint only after `Locked`, tutorial completion, terminal transition,
revive/new game and invisibility. Flush terminal state before publishing
`ToppedOut`. A failed ordinary checkpoint keeps play live; a failed terminal
flush still publishes a stable terminal result and records the failure through
the module's existing diagnostic/logging seam.

- [ ] **Step 4: Test stale frames and teardown**

Assert frames after terminal state, invisibility or Store disposal cannot move
the piece; rapid intents preserve order; lifecycle destruction cancels ticking
and performs no later mutation.

- [ ] **Step 5: Run and commit Store slice**

```bash
rtk ./gradlew :game:fallingblocks:allTests --tests '*TickPlannerTest' --tests '*FallingBlocksStore*'
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/component/game
rtk git commit -m "feat: coordinate falling blocks session state"
```

### Task 9: Wire retained Decompose components and stale-safe result slot

**Files:**
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/result/ResultComponent.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/result/DefaultResultComponent.kt`
- Replace: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/root/RootComponent.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/root/DefaultRootComponent.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/DefaultFallingBlocksComponent.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/component/result/DefaultResultComponentTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/component/root/RootComponentTest.kt`

- [ ] **Step 1: Write result countdown tests**

Copy no implementation from Block Blast; reuse its proven contract shape. Test
five active seconds, pause while inactive, one claimed action, failure reset,
one revive per run, and new-game phase after expiration:

```kotlin
assertEquals(5, component.model.value.continueSecondsRemaining)
visibility.set(MiniAppVisibility.INACTIVE)
scheduler.advanceTimeBy(2_000)
assertEquals(5, component.model.value.continueSecondsRemaining)
visibility.set(MiniAppVisibility.ACTIVE)
scheduler.advanceTimeBy(5_000)
assertFalse(component.model.value.isContinuePhase)
```

- [ ] **Step 2: Implement the result component contract**

```kotlin
internal interface ResultComponent {
    val model: Value<Model>
    fun onPrimaryClicked(requestContinue: (onApproved: () -> Unit) -> Unit)
    fun onContinueFailed()

    data class Model(
        val score: Long,
        val bestScore: Long,
        val canContinue: Boolean,
        val continueSecondsRemaining: Int,
    ) {
        val isContinuePhase get() = canContinue && continueSecondsRemaining > 0
    }
}
```

Use a component coroutine scope and `MiniAppVisibilitySource`; cancel the job
on destroy. `terminalActionHandled` permits exactly one in-flight primary action
and resets only on explicit continuation failure.

- [ ] **Step 3: Write root-slot lifecycle tests**

Assert Playing remains the identical object while the result child is active,
game input is frozen, back does not dismiss result, successful matching revive
dismisses only that slot, failed revive leaves it active, and callbacks from an
older run/session do nothing.

- [ ] **Step 4: Implement `SlotNavigation<ResultConfig>`**

```kotlin
interface RootComponent {
    val playing: FallingBlocksComponent
    val result: Value<ChildSlot<*, ResultComponent>>
    val frameMode: Value<MiniAppFrameMode>
    fun handleBack(): Boolean
}

@Serializable
private data class ResultConfig(val runId: Long, val score: Long, val bestScore: Long, val canContinue: Boolean)
```

Subscribe once to the Playing completion label, activate at most one matching
slot, and guard every child callback with both object identity and run ID.
`frameMode` stays `Standard` because the host toolbar and sharp live game remain
visible. `handleBack()` returns `true` while result is present without dismissing
it; otherwise delegate to Playing.

- [ ] **Step 5: Run and commit component navigation**

```bash
rtk ./gradlew :game:fallingblocks:allTests --tests '*ResultComponentTest' --tests '*RootComponentTest'
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/component
rtk git commit -m "feat: add resilient falling blocks result flow"
```

### Task 10: Implement density-independent gesture classification

**Files:**
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/input/GestureClassifier.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/input/FallingBlocksGestures.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/input/GestureClassifierTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/input/FallingBlocksGesturesTest.kt`

- [ ] **Step 1: Write classifier tests before pointer code**

```kotlin
@Test fun `tap rotates only when movement stays below slop`() {
    val classifier = classifier(density = 2f)
    classifier.down(x = 100f, y = 100f, timeMillis = 0)
    classifier.move(x = 104f, y = 103f, timeMillis = 40)
    assertEquals(listOf(GestureEvent.Rotate), classifier.up(104f, 103f, 80))
}

@Test fun `horizontal overshoot is retained between updates`() {
    val classifier = classifier(cellSizePx = 40f)
    classifier.down(0f, 0f, 0)
    assertEquals(listOf(GestureEvent.MoveHorizontal(1)), classifier.move(64f, 0f, 20))
    assertEquals(listOf(GestureEvent.MoveHorizontal(1)), classifier.move(80f, 0f, 40))
}

@Test fun `slow downward drag emits soft drop cells`() {
    val classifier = classifier(cellSizePx = 40f, hardDropVelocityPxPerSecond = 1_200f)
    classifier.down(0f, 0f, 0)
    assertEquals(listOf(GestureEvent.SoftDrop(2)), classifier.move(0f, 80f, 400))
    assertTrue(classifier.up(0f, 80f, 500).none { it is GestureEvent.HardDrop })
}

@Test fun `fast downward release emits one hard drop`() {
    val classifier = classifier(hardDropVelocityPxPerSecond = 1_200f)
    classifier.down(0f, 0f, 0)
    classifier.move(0f, 120f, 50)
    assertEquals(listOf(GestureEvent.HardDrop), classifier.up(0f, 180f, 75))
}

@Test fun `multi touch cancellation emits nothing`() {
    val classifier = classifier()
    classifier.down(0f, 0f, 0)
    assertTrue(classifier.cancelForAdditionalPointer().isEmpty())
    assertTrue(classifier.up(0f, 0f, 50).isEmpty())
}
```

Repeat physical motions at densities 1f, 2f and 3.5f and assert identical
logical events.

- [ ] **Step 2: Implement a pure sequence classifier**

```kotlin
internal sealed interface GestureEvent {
    data object Rotate : GestureEvent
    data class MoveHorizontal(val cells: Int) : GestureEvent
    data class SoftDrop(val cells: Int) : GestureEvent
    data object HardDrop : GestureEvent
}

internal class GestureClassifier(
    private val cellSizePx: Float,
    private val touchSlopPx: Float,
    private val hardDropVelocityPxPerSecond: Float,
)
```

Retain signed horizontal/downward remainders, lock the dominant axis after
slop, rate-limit soft drop by whole cells, and decide hard drop once at release.

- [ ] **Step 3: Bridge Compose pointer input without domain mutation**

`Modifier.fallingBlocksGestures(enabled, cellSize, onEvent)` creates one
classifier per pointer sequence, uses velocity tracking, cancels on multiple
pointers, and maps events to component methods. It contains no collision,
scoring, rotation or timing rules.

- [ ] **Step 4: Run UI-input tests**

```bash
rtk ./gradlew :game:fallingblocks:allTests --tests '*GestureClassifierTest' --tests '*FallingBlocksGesturesTest'
```

Expected: PASS; there is no Hold, button or side-zone event.

- [ ] **Step 5: Commit gestures**

```bash
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/input game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/input
rtk git commit -m "feat: add gesture only falling blocks controls"
```

### Task 11: Render the centered theme-derived CRT board and motion

**Files:**
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/board/BoardGeometry.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/board/TetrominoStyle.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/board/CrtBoard.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/motion/FallingBlocksMotionPolicy.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/screen/FallingBlocksScreen.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/board/BoardGeometryTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/board/TetrominoStyleTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/motion/FallingBlocksMotionPolicyTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/screen/FallingBlocksScreenTest.kt`

- [ ] **Step 1: Test board measurement independently**

```kotlin
@Test fun `board is centered in viewport regardless of edge overlay widths`() {
    val geometry = BoardGeometry.fit(viewport = Size(1200f, 800f), boardAspect = 0.5f)
    assertEquals(600f, geometry.bounds.center.x, 0.01f)
    assertEquals(400f, geometry.bounds.center.y, 0.01f)
}
```

Cover compact portrait, phone landscape, tablet, compact-height and banner-safe
viewport sizes. The calculation receives only the host-provided gameplay
viewport; preview/stats overlays never alter its center.

- [ ] **Step 2: Define semantic theme-role mapping**

```kotlin
internal data class PieceColors(val fill: Color, val highlight: Color, val outline: Color)
internal fun Tetromino.colors(scheme: ColorScheme): PieceColors = when (this) {
    Tetromino.I -> roles(scheme.primary, scheme.onPrimary)
    Tetromino.J -> roles(scheme.secondary, scheme.onSecondary)
    Tetromino.L -> roles(scheme.tertiary, scheme.onTertiary)
    Tetromino.O -> roles(scheme.primaryContainer, scheme.onPrimaryContainer)
    Tetromino.S -> roles(scheme.secondaryContainer, scheme.onSecondaryContainer)
    Tetromino.T -> roles(scheme.tertiaryContainer, scheme.onTertiaryContainer)
    Tetromino.Z -> roles(scheme.inversePrimary, scheme.inverseSurface)
}
```

Tests pass contrasting synthetic schemes and prove output changes with the
scheme. Do not use numeric `Color(...)` game literals.

- [ ] **Step 3: Draw board, pieces and restrained CRT layers**

Use one Canvas for settled cells, ghost and active piece. Clip all scanlines,
weak glow and minimal channel-offset passes to the board bounds. Scanline pitch
is density-aware. No random jitter or blur is applied to the whole gameplay
surface. Expose test tags `falling_blocks_board`, `falling_blocks_preview`,
`falling_blocks_level`, and `falling_blocks_lines`.

- [ ] **Step 4: Add event-driven motion policy**

```kotlin
internal data class FallingBlocksMotionPolicy(
    val moveMillis: Int,
    val rotateMillis: Int,
    val hardDropTrailMillis: Int,
    val impactMillis: Int,
    val lineSweepMillis: Int,
    val collapseMillis: Int,
    val resultMillis: Int,
)
```

Normal values are 70/110/90/140/180/160/280. Reduced motion returns zero for
translation/trail/collapse and uses only a short opacity fade. Static scanlines
remain. Render animation from transition IDs/timestamps; do not put an
`Animatable` or coroutine on each cell.

- [ ] **Step 5: Run and commit board UI**

```bash
rtk ./gradlew :game:fallingblocks:allTests --tests '*BoardGeometryTest' --tests '*TetrominoStyleTest' --tests '*MotionPolicyTest' --tests '*FallingBlocksScreenTest'
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/board game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/motion game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/screen/FallingBlocksScreen.kt game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui
rtk git commit -m "feat: render centered themed falling blocks board"
```

### Task 12: Add the mandatory four-step tutorial

**Files:**
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/tutorial/TutorialStep.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/tutorial/TutorialOverlay.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/store/FallingBlocksStore.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/store/FallingBlocksStoreFactory.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/screen/FallingBlocksScreen.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/tutorial/TutorialStepTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/tutorial/TutorialOverlayTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/component/game/store/TutorialFlowTest.kt`

- [ ] **Step 1: Write progression and no-skip tests**

```kotlin
enum class TutorialStep { ROTATE, MOVE, SOFT_DROP, HARD_DROP }

@Test fun `only matching legal gesture advances each tutorial step`() {
    val progress = TutorialProgress.initial()
    assertEquals(TutorialStep.ROTATE, progress.step)
    assertEquals(TutorialStep.ROTATE, progress.accept(GameFact.Moved(1, 0)).step)
    val moved = progress.accept(GameFact.Rotated).accept(GameFact.Moved(1, 0))
    assertEquals(TutorialStep.SOFT_DROP, moved.step)
    val dropped = moved.accept(GameFact.Moved(0, 1))
    assertEquals(TutorialStep.HARD_DROP, dropped.step)
    assertTrue(dropped.accept(GameFact.HardDropped(8)).complete)
}
@Test fun `tutorial model exposes no skip intent`() {
    assertFalse(FallingBlocksStore.Intent::class.sealedSubclasses.any { it.simpleName?.contains("Skip") == true })
}
@Test fun `completion persists seen then starts a clean scored zero run`() = runTest {
    val harness = tutorialStoreHarness(markSeenResult = Result.success(Unit))
    harness.completeRequiredGestures()
    assertEquals(listOf("tutorial_seen", "new_game"), harness.operations)
    assertEquals(0L, harness.store.state.game?.score)
    assertTrue(harness.store.state.game?.board?.isEmpty == true)
}
```

- [ ] **Step 2: Add deterministic practice state**

The tutorial uses a fixed board/piece sequence independent of the real run RNG.
Each step advances only after a matching legal `GameFact`; rejected rotations
and axis noise do not advance it. Block terminal top-out during practice.

- [ ] **Step 3: Render instructional motion with Material Symbols**

Use `Icons.Default.TouchApp` for tap and `Icons.AutoMirrored.Default.Swipe` or
the available matching Material symbol for drag. Draw the touch point, trail
and arrow with theme colors. Do not import supplied hand imagery. Reduced motion
shows the icon and static arrow without looping translation.

- [ ] **Step 4: Persist completion before creating the real run**

`TutorialSeenRepository.markSeen()` must succeed or be retried through the
session coordinator before replacing practice state. The real run is newly
seeded, score zero, empty board and untouched by practice moves.

- [ ] **Step 5: Run and commit tutorial**

```bash
rtk ./gradlew :game:fallingblocks:allTests --tests '*Tutorial*'
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/tutorial game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/screen/FallingBlocksScreen.kt game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks
rtk git commit -m "feat: add mandatory falling blocks tutorial"
```

### Task 13: Compose the result overlay, host toolbar and ad gate

**Files:**
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/result/ResultOverlay.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/screen/root/RootTopBarContent.kt`
- Replace: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/screen/root/RootContent.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/FallingblocksSession.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/result/ResultOverlayTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/FallingblocksSessionTest.kt`

- [ ] **Step 1: Write UI contract tests**

Assert the terminal board remains in the tree, result has no dismiss action,
back is consumed, scrim alpha is zero, no blur modifier is applied, primary
semantics disclose an advertisement during Try Again, countdown appears, and
New Game replaces it after expiration.

- [ ] **Step 2: Render a non-dismissible bottom-anchored surface**

Use a full-size `Box`: render `FallingBlocksScreen` first, then align a Material
`Surface` to `BottomCenter`. Do not use `ModalBottomSheet`, `Dialog`, blur, dim
scrim or outside-click dismissal. Animate only the panel entrance according to
the motion policy.

- [ ] **Step 3: Connect the generic ad gate in Compose**

```kotlin
val gate = ads.rememberGate(MiniAppAdKind.Fullscreen("continue_after_game_over"))
ResultOverlay(
    model = result.model,
    onPrimary = { result.onPrimaryClicked(gate.request) },
    advertisementExpected = gate.willShowAd,
)
```

The public gate is exactly-once completion based and has no game-facing failure
callback. When policy, entitlement, readiness or load state means no ad will be
shown, the host completes the gate immediately. `onContinueFailed` is reserved
for a matching revive/commit failure after gate completion; it must not attempt
to infer native advertisement state.

- [ ] **Step 4: Add toolbar and session composition**

`RootTopBarContent` subscribes to Playing model and displays localized Score and
Best without owning state. `FallingblocksSession` extends
`DelegatingMiniAppSession(frameMode = component.frameMode, wantsBanner = true,
onBack = component::handleBack)`, renders the top bar, and passes
`MiniAppAdsCapability` to `RootContent`.

- [ ] **Step 5: Run and commit terminal UI**

```bash
rtk ./gradlew :game:fallingblocks:allTests --tests '*ResultOverlayTest' --tests '*FallingblocksSessionTest' --tests '*RootComponentTest'
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/result game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/screen/root game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/FallingblocksSession.kt game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks
rtk git commit -m "feat: add falling blocks terminal presentation"
```

### Task 14: Author and connect original adaptive procedural audio

**Files:**
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/audio/FallingBlocksAudio.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/audio/FallingBlocksProgram.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/audio/FallingBlocksAudioAdapter.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/store/FallingBlocksStoreFactory.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/di/FallingblocksSessionBindings.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/audio/FallingBlocksAudioTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/audio/FallingBlocksAudioRenderTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/audio/FallingBlocksAudioAdapterTest.kt`

- [ ] **Step 1: Define typed roles and failing routing tests**

```kotlin
internal object FallingBlocksAudio {
    val Music = MusicName("falling_blocks_music")
    val Move = SfxName("move")
    val Rotate = SfxName("rotate")
    val SoftDrop = SfxName("soft_drop")
    val HardDrop = SfxName("hard_drop")
    val Lock = SfxName("lock")
    val Line1 = SfxName("line_1")
    val Line2 = SfxName("line_2")
    val Line3 = SfxName("line_3")
    val Line4 = SfxName("line_4")
    val Perfect = SfxName("perfect")
    val LevelUp = SfxName("level_up")
    val GameOver = SfxName("game_over")
    val Revive = SfxName("revive")
}
```

Test each domain fact maps once, movement and soft-drop events are rate-limited,
and intensity changes only when level band or stack-height band changes.

- [ ] **Step 2: Author the original 126 BPM program**

Compose `ChipLead` and `AnalogBass` preset fragments with an original motif and
game-owned pulse/noise drums where necessary. Use fixed seeds. Base intensity
contains bass/percussion; higher bands add lead/density without changing
transport BPM. Include `WoodenPlacementThock(FallingBlocksAudio.Lock)`, tuned
`SuccessSweep` for four/perfect clears and tuned `PowerUp` for revive. Author
original rotate, hard-drop, ordinary clear, level-up and descending game-over
declarations. Do not encode the supplied Sprudel note/rhythm sequence.

- [ ] **Step 3: Add deterministic acoustic assertions**

Render representative base/high loops and every SFX. Assert successful
validation, identical fixed-seed features, peak below the repository headroom
limit, non-silent RMS, bounded event density, distinct line-clear energy/centroid
progression, no long DC offset and acceptable mobile work estimate.

- [ ] **Step 4: Bind the session facade and Store adapter**

`FallingBlocksAudioAdapter` is the only type that sends commands to
`MiniAppAudio`. The Store emits semantic calls, never SFX names or DSP commands.
Start music after bootstrap/tutorial entry, set intensity on band changes, and
let host lifecycle/visibility/ad suppression handle muting and teardown. Drop
rejected commands without retry loops.

- [ ] **Step 5: Run and commit audio**

```bash
rtk ./gradlew :game:fallingblocks:allTests --tests '*Audio*'
rtk ./gradlew :miniapp:audio-presets:allTests
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/audio game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/store/FallingBlocksStoreFactory.kt game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/di/FallingblocksSessionBindings.kt game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/audio
rtk git commit -m "feat: add falling blocks procedural audio"
```

### Task 15: Complete Metro wiring, integration tests and acceptance evidence

**Files:**
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/di/FallingblocksSessionGraph.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/di/FallingblocksSessionBindings.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/FallingblocksPlugin.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/di/FallingblocksSessionGraphTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/FallingblocksLifecycleIntegrationTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/FallingblocksThemeIntegrationTest.kt`
- Modify: `docs/miniapp/proposals/game.fallingblocks/acceptance.md`
- Modify: `game/fallingblocks/PROVENANCE.md`
- Do not modify: `settings.gradle.kts`

- [ ] **Step 1: Wire the retained graph with namespaced factory**

Keep the generated factory name exactly:

```kotlin
@ContributesTo(AppScope::class)
@GraphExtension.Factory
fun interface Factory {
    fun createGameFallingblocksSessionGraph(
        @Provides context: MiniAppSessionContext,
    ): FallingblocksSessionGraph
}
```

Provide app-scoped persistence adapters and session-scoped Store, component,
root, result, audio adapter and session through the game binding container only.

- [ ] **Step 2: Add graph and lifecycle integration tests**

Assert isolated registry discovery, one retained child graph per session, exact
state across lifecycle recreation, invisible tick suspension, final checkpoint,
stale ad rejection after destruction, audio command suppression through host
lifecycle, `wantsBanner == true`, and no platform/feature/other-game dependency.

- [ ] **Step 3: Add adaptive/theme/accessibility integration tests**

Capture/assert compact portrait, wide, compact-height and tablet measurements;
light/dark synthetic schemes; reduced motion; each tutorial step; blocked
rotation; sharp undimmed game-over board; 48 dp result action; localized English
semantics; and absence of Hold/Skip controls.

- [ ] **Step 4: Run the full affected automated verification**

Run in this order and record exact outcomes in `acceptance.md`:

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

Expected: every command exits 0. Confirm `settings.gradle.kts` has no diff and
the module is discoverable but absent from `:miniapp:bundle` dependencies.

- [ ] **Step 5: Perform experiential acceptance and update evidence**

On available Android and iOS targets, inspect the visual/motion matrix listed in
`acceptance.md`. Render and listen to base/high music, every SFX, repeated SFX
over music, mute, background/resume, fullscreen-ad suppression and teardown.
Record artifact paths, device/simulator coverage, unavailable checks and honest
limitations. Do not mark aesthetic criteria passed from compilation alone.

- [ ] **Step 6: Commit the completed non-shipping MiniApp**

```bash
rtk git add game/fallingblocks docs/miniapp/proposals/game.fallingblocks/acceptance.md
rtk git commit -m "test: verify falling blocks miniapp"
```

Final handoff must state `NOT ALLOWLISTED`, give exact verification results,
link visual/audio artifacts, list unavailable device checks, and confirm the
production allowlist was not changed.
