# Falling Blocks UX Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Correct the reviewed Falling Blocks gameplay presentation by introducing a distinct Block Blast-style result screen, a smaller centered board with one external preview, reliable ghost contrast, event-driven hard-drop/line-clear effects, and UIKit-styled Score/Best metrics.

**Architecture:** Replace the result `ChildSlot` with a serializable Decompose `ChildStack` whose Playing child remains retained below a Result destination. Keep rules pure; enrich typed engine facts only with cleared row identities, convert facts into transient presentation events in the MVIKotlin layer, and render those events through one board-level animation state. Compose owns sizing and drawing, while the host top bar owns Score/Best presentation.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform/Material 3, Decompose ChildStack, MVIKotlin, Metro, kotlinx.serialization, `core:uikit`, Compose UI tests, kotlin-test.

---

## Preconditions and Locked Decisions

- Work directly in the existing `codex/fallingblocks` checkout. Do not create a worktree.
- Prefix every shell command with `rtk`.
- Preserve unrelated untracked Firebase SwiftPM directories.
- Do not modify the production `miniApps` allowlist in `settings.gradle.kts`; the result remains **NOT ALLOWLISTED**.
- Keep gesture-only input, no Hold, no gameplay buttons, one endless mode, mandatory English tutorial, `wantsBanner = true`, procedural audio, and theme-only colors.
- Treat `/Users/yet/Downloads/2026-09-21 10.36.17.jpg` as visual layout evidence only. Do not copy its colors, branding, pause control, target/moves mechanics, or art.
- Follow TDD for every task: add the named failing test, run it and observe the intended failure, implement the smallest slice, rerun the narrow test, then commit.
- Do not introduce a dependency on `:game:blockblast` or `:game:fruitmerge`. Reuse public `:core:uikit` components; adapt game-local concepts when an existing game implementation is internal.

## Target Screen Contract

### Playing

```text
host toolbar:       [ SCORE / BEST UIKit presentation ]

game viewport:                         ┌──────── NEXT ────────┐
                                       │ one preview piece   │
                                       └─────────────────────┘
                  ┌──────────────────────────────────┐
                  │                                  │
                  │      centered 10 × 20 board      │
                  │                                  │
                  └──────────────────────────────────┘
```

- The board center equals the game viewport center on compact portrait, wide,
  tablet, and compact-height layouts.
- `BoardGeometry.fit` receives `edgeInset = 24dp`, a symmetric support reserve
  of `64dp` when height is at least `700dp` and `48dp` below it, plus a hard
  `maxBoardWidth = 340dp`. Board width is:

```kotlin
minOf(
    viewportWidth - 2f * edgeInset,
    (viewportHeight - 2f * supportReserve) * Board.WIDTH / Board.VISIBLE_HEIGHT,
    maxBoardWidth,
)
```

- The single `Next` card is `96dp × 56dp`, sits `8dp` above the board, and its
  right edge aligns with the board's right edge. If the compact-height viewport
  cannot fit that location, clamp the card inside the viewport without moving
  the board.
- No Score, Level, or Lines element is a descendant/overlay of the board.

### Result

- Result is a separate `ContentOnly` destination, not an overlay or dismissible
  bottom sheet.
- Portrait: title, centered final 10×20 board, then Block Blast-style result
  card with Score, Best, countdown/CTA.
- Landscape: final board on the left, title and result card on the right.
- The final board remains sharp and theme-colored, with no blur/dim layer.
- Back is consumed while Result is active. Continue may return to the retained
  Playing child only after revive succeeds. New Game replaces the full stack.

## Motion Contract

| Trigger | Visible change | Duration | Interruption | Reduced motion |
|---|---|---:|---|---|
| Hard drop | theme-derived RGB/channel split on the dropped piece, vertical afterimage trail, then board-border impact pulse | 90ms trail + 140ms impact | newer event cancels previous board effect | 80ms opacity pulse |
| Line clear | cleared rows flash, emit bounded square particles, then expose collapsed board | 180ms flash/burst + 160ms collapse cue | one event state; later clear replaces it | 90ms row opacity flash |
| Result navigation | Decompose fade between destinations | 180ms | navigation owns cancellation | immediate/short fade |

No per-cell coroutine or `Animatable` is allowed. One board-level controller
drives all overlay drawing. Effects are keyed by a monotonic event ID and do
not replay on ordinary recomposition.

## Task 1: Replace the result overlay with a retained ChildStack destination

**Files:**
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/result/FallingBlocksResultSnapshot.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/root/RootComponent.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/root/DefaultRootComponent.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/result/ResultComponent.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/result/DefaultResultComponent.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/FallingBlocksComponent.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/DefaultFallingBlocksComponent.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/store/FallingBlocksStoreFactory.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/component/root/RootComponentTest.kt`
- Test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/component/result/FallingBlocksResultSnapshotTest.kt`
- Update: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/component/result/DefaultResultComponentTest.kt`

- [x] **Step 1: Add failing snapshot tests**

Define tests proving that a terminal state maps to a serializable immutable
snapshot containing all 220 board cells, active type/rotation/origin, score,
best score, run ID, and revive count. Round-trip through
`Json.encodeToString/decodeFromString` and assert equality. Reject malformed
cell counts and invalid enum ordinals.

- [x] **Step 2: Add failing root navigation tests**

Assert these exact state transitions:

```kotlin
assertIs<RootComponent.Child.Playing>(root.stack.value.active.instance)
harness.topOut()
assertIs<RootComponent.Child.Result>(root.stack.value.active.instance)
assertEquals(MiniAppFrameMode.ContentOnly, root.frameMode.value)
assertTrue(root.handleBack())

harness.approveContinue()
assertIs<RootComponent.Child.Playing>(root.stack.value.active.instance)
assertSame(originalPlaying, root.stack.value.active.instance.component)

harness.topOutAgainWithoutRevive()
harness.startNewGame()
assertNotSame(originalPlaying, root.stack.value.active.instance.component)
assertEquals(1, root.stack.value.items.size)
```

Also retain the existing stale-ad callback test: approval for a destroyed
Result child must not revive or replace its successor.

- [x] **Step 3: Implement the stack model**

Use this public root shape:

```kotlin
internal interface RootComponent {
    val stack: Value<ChildStack<*, Child>>
    val frameMode: Value<MiniAppFrameMode>
    fun handleBack(): Boolean

    sealed interface Child {
        class Playing(val component: FallingBlocksComponent) : Child
        class Result(val component: ResultComponent) : Child
    }
}
```

Use serializable `Config.Playing(instanceId, isNewGame)` and
`Config.Result(gameInstanceId, snapshot, canContinue)`. Push Result exactly
once for the matching run. Continue calls `revive()` on the retained matching
Playing child and removes Result only after its model returns to `PLAYING`.
New Game uses `replaceAll` with a new instance ID and creates Playing with
`startFresh = true`. The Store bootstrap must skip the restored game snapshot
for that child while retaining persisted Best and tutorial completion; this
avoids a destroy/checkpoint race. Map Playing to `Standard` frame mode and
Result to `ContentOnly`.

- [x] **Step 4: Pass the snapshot through ResultComponent**

Replace scalar `score`/`bestScore` factory inputs with
`snapshot: FallingBlocksResultSnapshot`. Keep the existing visibility-aware
five-second countdown and exactly-once primary gate unchanged.

Implementation note: `isNewBest` is deliberately not added. The persisted
model does not retain a round-start best baseline, and the requested polish is
Score/Best styling rather than a new-best badge. Adding it would require an
unrelated persistence-schema migration.

- [x] **Step 5: Run and commit**

```bash
rtk ./gradlew :game:fallingblocks:allTests
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/component
rtk git commit -m "refactor: navigate to falling blocks result screen"
```

Expected: PASS; the old `ChildSlot` API is absent.

**Implementation outcome (2026-09-21):** migrated the root from `ChildSlot`
to a retained `ChildStack`, added a validated serializable terminal snapshot,
and made Result a real `ContentOnly` destination. Continue returns to the same
Playing component after a successful revive; New Game replaces the stack with
a fresh Playing child that intentionally skips the saved game snapshot while
retaining Best and tutorial completion. The legacy `ResultOverlay` is used only
as the temporary renderer for the new destination until Task 2 replaces it.
`rtk ./gradlew :game:fallingblocks:allTests` passed.

## Task 2: Build the adaptive Block Blast-style result screen

**Files:**
- Delete: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/result/ResultOverlay.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/result/FallingBlocksResultContent.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/result/ResultCard.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/result/ResultLayoutPolicy.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/board/CrtBoard.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/screen/root/RootContent.kt`
- Replace test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/result/ResultOverlayTest.kt` → `FallingBlocksResultContentTest.kt`
- Create test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/result/ResultLayoutPolicyTest.kt`

- [x] **Step 1: Add failing layout-budget tests**

Cover `320×568`, `360×640`, `400×800`, `800×400`, and `1200×800`. Assert
the 1:2 result board and complete CTA fit; button height is at least `48dp`;
portrait stacks board/card and landscape uses two panes. Cap the result board
at `280dp` wide / `560dp` high so a tall field never pushes the CTA offscreen.

- [x] **Step 2: Add failing Compose result tests**

Assert title, exact Score/Best values, final board, five-second Continue CTA,
advertisement semantics, New Game after expiry, and absence of overlay/panel
test tags. Assert no blur/scrim node exists and the result action is at least
48dp tall.

- [x] **Step 3: Make CrtBoard support read-only terminal rendering**

Add explicit parameters with safe defaults:

```kotlin
internal fun CrtBoard(
    state: FallingBlocksState,
    spatialEffectsEnabled: Boolean,
    showGhost: Boolean = true,
    modifier: Modifier = Modifier,
)
```

Result uses `showGhost = false`, no gestures, and no transient effects. Render
the validated snapshot's Board and ActivePiece directly through a read-only
`CrtBoard` overload so no incomplete synthetic gameplay state is invented.

- [x] **Step 4: Adapt the Block Blast result composition, not its game types**

Use the same structural pattern as `GameResultContent`: responsive portrait/
landscape layout, theme-derived title, final-board pane, elevated rounded result
card, Score/Best styling, and one primary CTA. Keep Falling Blocks'
existing Continue/New Game behavior and strings. Do not import Block Blast.

- [x] **Step 5: Render root destinations through Decompose Children**

```kotlin
Children(stack = stack, animation = stackAnimation(fade())) { child ->
    when (val instance = child.instance) {
        is RootComponent.Child.Playing -> FallingBlocksScreen(instance.component)
        is RootComponent.Child.Result -> FallingBlocksResultContent(
            component = instance.component,
            interstitialGate = ads.rememberGate(
                MiniAppAdKind.Fullscreen("continue_after_game_over"),
            ),
        )
    }
}
```

- [x] **Step 6: Run and commit**

```bash
rtk ./gradlew :game:fallingblocks:allTests
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui
rtk git commit -m "feat: add falling blocks result screen"
```

**Implementation outcome (2026-09-21):** deleted the bottom-sheet-style
overlay and added a real adaptive result destination. Portrait layouts stack a
theme-colored title, read-only 1:2 terminal board, and elevated Score/Best CTA
card; landscape and expanded widths use two panes. The board is capped at
`280×560dp`, the CTA keeps a measured `48dp` minimum, Continue retains its
five-second timer and advertisement semantics, and the terminal render has no
scrim, blur, ghost, gestures, or transient effects. Layout budgets cover all
five requested viewport sizes. `rtk ./gradlew :game:fallingblocks:allTests`
passed (103 tests).

## Task 3: Resize the centered field and simplify gameplay chrome

**Files:**
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/board/BoardGeometry.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/screen/game/FallingBlocksScreen.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/screen/root/RootTopBarContent.kt`
- Update: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/board/BoardGeometryTest.kt`
- Update: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/screen/game/FallingBlocksScreenTest.kt`
- Update: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/FallingblocksThemeIntegrationTest.kt`

- [ ] **Step 1: Replace the old geometry expectations with failing bounds**

Extend `BoardGeometry.fit` with `supportReserve` and `maxBoardWidth`. Assert:

```kotlin
val phone = BoardGeometry.fit(400f, 800f, 24f, 64f, 340f)
assertEquals(336f, phone.width, 0.01f)
assertEquals(200f, phone.centerX, 0.01f)
assertEquals(400f, phone.centerY, 0.01f)

val compact = BoardGeometry.fit(360f, 640f, 24f, 48f, 340f)
assertEquals(272f, compact.width, 0.01f)
assertEquals(180f, compact.centerX, 0.01f)
assertEquals(320f, compact.centerY, 0.01f)
```

Also cover wide/tablet and prove the board never exceeds `340dp`, never leaves
the viewport, and keeps the 1:2 aspect.

- [ ] **Step 2: Write the simplified screen test**

Assert one preview canvas exists, its bounds are above/outside the board, and
its right edge aligns with the board. Assert the screen tree has no Level or
Lines tags and no in-board Score text. Keep the `Hold` absence assertion.

- [ ] **Step 3: Implement the new playing layout**

Delete `Hud`, `Metric`, the Level/Lines test tags, and the five-item preview
loop. Render `state.preview.firstOrNull()` only. Use the exact geometry contract
above and place the preview relative to `geometry.right/top`, clamped to the
viewport. Apply gestures only to the board bounds.

- [ ] **Step 4: Style Score/Best with shared UIKit**

Replace plain `"Score 0"`/`"Best 0"` toolbar text with
`core:uikit`'s `CompactScoreCard`, localized labels, current score, and best
score. Constrain it to the host center slot without creating a nested theme.
Do not show Level or Lines in the host toolbar.

- [ ] **Step 5: Run and commit**

```bash
rtk ./gradlew :game:fallingblocks:allTests
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/board/BoardGeometry.kt game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/screen game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/FallingblocksThemeIntegrationTest.kt
rtk git commit -m "feat: refine falling blocks gameplay layout"
```

## Task 4: Make the landing ghost reliably visible

**Files:**
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/board/CrtBoard.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/board/GhostStyle.kt`
- Create test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/board/GhostStyleTest.kt`
- Update test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/board/TetrominoStyleTest.kt`

- [ ] **Step 1: Reproduce the actual visibility bug in a pure style test**

The current implementation passes `style.fill.copy(alpha = 0.14f)` as the
color of a `Stroke`, so the outline itself is only 14% opaque and there is no
filled ghost. Add a test requiring separate fill and outline roles:

```kotlin
assertTrue(style.fill.alpha in 0.18f..0.28f)
assertTrue(style.outline.alpha >= 0.62f)
assertTrue(contrastRatio(style.outline.compositeOver(board), board) >= 3f)
```

Run the test and observe failure with the old single-color design.

- [ ] **Step 2: Implement theme-derived GhostStyle**

Choose the stronger of the tetromino outline and `colorScheme.onSurface` after
compositing against `surfaceContainerLowest`. Return a translucent fill,
high-contrast outline, and stroke width. No hard-coded game color literals.

- [ ] **Step 3: Draw both fill and outline**

Draw a translucent rounded fill first, then a mostly solid dashed outline with
a minimum density-aware width. Keep ghost cells below settled/active cells and
hide them when the landing origin equals the active origin.

- [ ] **Step 4: Run and commit**

```bash
rtk ./gradlew :game:fallingblocks:allTests
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/board game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/board
rtk git commit -m "fix: improve falling blocks landing ghost"
```

## Task 5: Expose deterministic transient presentation events

**Files:**
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/model/GameTransition.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain/engine/FallingBlocksEngine.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/FallingBlocksVisualEvent.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/FallingBlocksTransitionPlanner.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/store/FallingBlocksStore.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/store/FallingBlocksStoreFactory.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/FallingBlocksComponent.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component/game/mapper/Mappers.kt`
- Update tests: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/engine/LineClearTest.kt`
- Create test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/component/game/FallingBlocksTransitionPlannerTest.kt`
- Update test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/component/game/store/FallingBlocksStoreTest.kt`

- [ ] **Step 1: Make cleared rows explicit**

Change the fact to:

```kotlin
data class LinesCleared(
    val rows: List<Int>,
    val perfect: Boolean,
) : GameFact {
    val count: Int get() = rows.size
}
```

Engine tests must assert the exact sorted row indices before collapse, not only
the count. Update audio/tutorial consumers to use `count` so sound behavior is
unchanged.

- [ ] **Step 2: Define immutable visual events**

```kotlin
internal sealed interface FallingBlocksVisualEvent {
    val id: Long

    data class HardDrop(
        override val id: Long,
        val type: Tetromino,
        val from: List<Cell>,
        val to: List<Cell>,
    ) : FallingBlocksVisualEvent

    data class LineClear(
        override val id: Long,
        val rows: List<Int>,
        val cells: List<VisualCell>,
    ) : FallingBlocksVisualEvent
}
```

`VisualCell` contains cell/type only. It is transient and is never included in
the persistence schema.

- [ ] **Step 3: Add a pure planner**

Given `before`, `action`, `after`, `facts`, and `nextId`, return at most one
event. Hard drop captures active and landing cells from `before`. A line clear
takes precedence and captures the complete pre-collapse cleared rows, including
the just-locked active piece. Normal movement/ticks return `null`.

- [ ] **Step 4: Publish event state exactly once per transition**

Add `visualEvent: FallingBlocksVisualEvent?` and `nextVisualEventId: Long` to
Store state/model. Update them in the same reducer message as the game state.
Initialization/restoration starts with `null`; recomposition does not allocate
a new ID. Verify IDs are monotonic and overflow is rejected before wraparound.

- [ ] **Step 5: Run and commit**

```bash
rtk ./gradlew :game:fallingblocks:allTests
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/domain game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/component game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks
rtk git commit -m "feat: expose falling blocks visual events"
```

## Task 6: Render hard-drop CRT and Block Blast-inspired clear effects

**Files:**
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/board/FallingBlocksBoardEffects.kt`
- Create: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/board/BoardEffectGeometry.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/board/CrtBoard.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/motion/FallingBlocksMotionPolicy.kt`
- Modify: `game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui/screen/game/FallingBlocksScreen.kt`
- Create test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/board/BoardEffectGeometryTest.kt`
- Update test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/motion/FallingBlocksMotionPolicyTest.kt`
- Update test: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui/screen/game/FallingBlocksScreenTest.kt`

- [ ] **Step 1: Add pure geometry and motion tests**

Assert every particle/trail coordinate is normalized from a 10×20 visible
board, hidden rows are clipped, particle count is bounded (`<= 8` per cleared
cell but `<= 96` total), and fixed event IDs produce deterministic particle
directions. Assert normal and reduced-motion durations match the Motion Contract.

- [ ] **Step 2: Implement one board-level effect controller**

Use one `Animatable<Float>` or one transition at the board wrapper, keyed by
`visualEvent.id`. `collectLatest`/`LaunchedEffect(id)` cancels the old animation
when a newer event arrives. Do not mutate the Store and do not create animation
objects inside cell loops.

- [ ] **Step 3: Draw the hard-drop effect**

During the first 90ms, draw the captured dropped piece as fading vertical
afterimages and small opposite horizontal channel offsets using
`primary`/`tertiary` alpha layers. During the next 140ms, pulse the board border
and landing cells. Clip every pass to board bounds; do not glitch the host or
the whole game viewport.

- [ ] **Step 4: Draw the line-clear effect**

Adapt Block Blast's causal language without importing its 8×8 implementation:
flash exact cleared rows, emit deterministic theme-colored square particles,
and draw a restrained shock line. The already-collapsed board stays underneath;
captured pre-collapse cells fade out on top, making collapse understandable.

- [ ] **Step 5: Implement reduced motion**

When `spatialMotionEnabled` is false, skip trails, channel displacement,
particles, and translation. Draw only an 80–90ms alpha pulse/row flash.

- [ ] **Step 6: Run and commit**

```bash
rtk ./gradlew :game:fallingblocks:allTests
rtk git add game/fallingblocks/src/commonMain/kotlin/ge/yet/game/fallingblocks/ui game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/ui
rtk git commit -m "feat: animate falling blocks board events"
```

## Task 7: Integration, regression, and acceptance evidence

**Files:**
- Update: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/FallingblocksLifecycleIntegrationTest.kt`
- Update: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/FallingblocksThemeIntegrationTest.kt`
- Update: `game/fallingblocks/src/commonTest/kotlin/ge/yet/game/fallingblocks/FallingblocksPluginContractTest.kt`
- Update: `docs/miniapp/proposals/game.fallingblocks/acceptance.md`
- Update: `game/fallingblocks/PROVENANCE.md` only if implementation introduces new original visual declarations
- Do not modify: `settings.gradle.kts`

- [ ] **Step 1: Add end-to-end component assertions**

Verify exact checkpoint restore starts with no stale visual event; backgrounding
pauses countdown/ticks/effects; destroyed Result ad approval is stale; successful
revive returns to the retained Playing child; New Game destroys old children;
banner opt-in remains true; Result uses `ContentOnly` and Playing uses `Standard`.

- [ ] **Step 2: Add the final theme/layout matrix**

Test light/dark synthetic schemes at `320×568`, `360×640`, `400×800`,
`800×400`, and `1200×800`. Assert centered/capped field, one external preview,
no in-field metrics, readable ghost style, 48dp actions, no Hold/Skip, and
English semantics.

- [ ] **Step 3: Run focused and broad automated verification**

```bash
rtk ./gradlew :game:fallingblocks:allTests
rtk ./gradlew :game:fallingblocks:validateMiniAppDependencies
rtk ./gradlew :game:fallingblocks:compileAndroidMain
rtk ./gradlew :game:fallingblocks:compileKotlinIosSimulatorArm64
rtk ./gradlew :game:fallingblocks:verifyMiniApp
rtk ./gradlew :game:blockblast:allTests
rtk ./gradlew :core:uikit:allTests
rtk ./gradlew :composeApp:compileAndroidMain
rtk git diff --check
rtk git diff --exit-code -- settings.gradle.kts
```

Expected: every command exits 0; Falling Blocks remains discoverable and absent
from production bundle dependencies.

- [ ] **Step 4: Perform device experience checks**

On an available Android emulator/device, record portrait gameplay and Result.
Inspect: margins; field center; one Next piece; ghost over empty/dense bottoms;
hard drop; one/four-line clear; reduced motion; Continue countdown/ad return;
New Game; banner mounted/unmounted viewport changes. Repeat the critical layout
and lifecycle checks on iOS when available. Mark missing device checks
`blocked-by-environment`, never `passed` from compilation.

- [ ] **Step 5: Update acceptance evidence and commit**

Record exact commands, screenshots/video paths, observed defects, and
unavailable checks in `acceptance.md`.

```bash
rtk git add game/fallingblocks docs/miniapp/proposals/game.fallingblocks
rtk git commit -m "test: verify falling blocks ux polish"
```

Final handoff must explicitly state **NOT ALLOWLISTED**, confirm no
`settings.gradle.kts` diff, and list any unperformed human visual/audio checks.

## Self-Review Checklist

- Requirement 1 is covered by Tasks 1–2: separate retained Result destination
  using the Block Blast structure and the existing Falling Blocks ad/countdown.
- Requirements 2–3 are covered by Task 3: smaller centered board, margins, one
  Next preview outside it, and removal of Score/Level/Lines from the field.
- Requirement 4 is covered by Task 4 with the identified 14%-stroke root cause.
- Requirement 5 is covered by Tasks 5–6 with typed event IDs, hard-drop CRT,
  line-clear particles, cancellation, and reduced motion.
- Requirement 6 is covered by Task 3 through the public UIKit score component.
- Existing gesture-only, tutorial, audio, ads, persistence, theme, banner, and
  non-allowlisted constraints remain explicit and receive regression coverage.
