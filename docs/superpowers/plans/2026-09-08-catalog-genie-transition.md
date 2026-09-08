# Catalog Genie Transition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a reversible macOS-inspired Genie transition between a selected catalog card and its Running MiniApp while preserving Decompose navigation and session lifecycle guarantees.

**Architecture:** Decompose remains the source of truth for the Root stack. Catalog presentation reports window-space card bounds to `RootContent`; a Root-owned Compose coordinator drives a pure, tested strip-geometry transform and delays a close commit until the exit animation finishes. Compose records one graphics layer per transition and a Canvas draws its horizontal slices; reduced motion and capture/geometry failures use a bounded affine fallback.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.12, Decompose 3.5, Essenty BackHandler, `GraphicsLayer`, Compose Canvas, `kotlin.test`, Compose UI tests.

---

## File Map

- Create `composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition/GenieGeometry.kt`: platform-neutral band geometry and endpoint validation.
- Create `composeApp/src/commonTest/kotlin/ge/yet/game/screen/root/transition/GenieGeometryTest.kt`: deterministic geometry tests.
- Create `composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition/GenieTransitionState.kt`: transition phases, direction, target and pure state reduction.
- Create `composeApp/src/commonTest/kotlin/ge/yet/game/screen/root/transition/GenieTransitionStateTest.kt`: duplicate, completion and cancellation tests.
- Modify `feature/catalog/src/commonMain/kotlin/ge/yet/game/feature/catalog/ui/MiniAppListItemCard.kt`: measure card bounds and report them when Play is pressed.
- Modify `feature/catalog/src/commonMain/kotlin/ge/yet/game/feature/catalog/ui/CatalogContent.kt`: thread the presentation-only launch callback through the grid.
- Modify `feature/catalog/src/commonTest/kotlin/ge/yet/game/feature/catalog/ui/CatalogContentTest.kt`: verify ID and measured bounds delivery.
- Modify `feature/root/src/commonMain/kotlin/ge/yet/game/feature/root/RootComponent.kt`: add a two-phase MiniApp Back contract.
- Modify `feature/root/src/commonMain/kotlin/ge/yet/game/feature/root/DefaultRootComponent.kt`: offer Back to the session once, then commit close after animation.
- Modify `feature/root/src/commonTest/kotlin/ge/yet/game/feature/root/DefaultRootComponentTest.kt`: lock lifecycle ordering and stale-token behavior.
- Create `composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition/GenieTransitionCoordinator.kt`: Compose-facing state, timing and capture lifecycle.
- Create `composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition/GenieTransitionOverlay.kt`: graphics-layer recording and Canvas strip drawing.
- Create `composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition/RootGenieContent.kt`: Root/Decompose integration and input gating.
- Modify `composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/RootContent.kt`: install `RootGenieContent` for Catalog/Running pairs and preserve the ordinary fallback for other pairs.
- Modify `composeApp/src/commonTest/kotlin/ge/yet/game/screen/root/RootContentTest.kt`: verify launch/back behavior, semantics, cancellation and fallback.
- Modify `composeApp/src/commonMain/kotlin/ge/yet/game/utils/CupertinoPredictiveBack.kt`: expose the existing safe predictive-back progress seam for the Root Genie coordinator without changing Settings transitions.

## Task 1: Pure Genie Geometry

**Files:**
- Create: `composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition/GenieGeometry.kt`
- Test: `composeApp/src/commonTest/kotlin/ge/yet/game/screen/root/transition/GenieGeometryTest.kt`

- [ ] **Step 1: Write endpoint and validity tests**

Create tests using fixed `Rect` values. The assertions must cover all bands rather than sampling one:

```kotlin
package ge.yet.game.screen.root.transition

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenieGeometryTest {
    private val viewport = Rect(0f, 0f, 1080f, 2400f)
    private val card = Rect(72f, 640f, 1008f, 816f)

    @Test
    fun endpoints_match_viewport_and_card_exactly() {
        val start = calculateGenieBands(viewport, card, progress = 0f, bandCount = 20)
        val end = calculateGenieBands(viewport, card, progress = 1f, bandCount = 20)

        assertEquals(viewport, start.envelope)
        assertEquals(card, end.envelope)
        assertEquals(viewport.top, start.bands.first().destination.top)
        assertEquals(viewport.bottom, start.bands.last().destination.bottom)
        assertEquals(card.top, end.bands.first().destination.top)
        assertEquals(card.bottom, end.bands.last().destination.bottom)
    }

    @Test
    fun every_intermediate_band_is_finite_non_negative_and_bounded() {
        (0..100).forEach { step ->
            val geometry = calculateGenieBands(viewport, card, step / 100f, 20)
            geometry.bands.forEach { band ->
                val rect = band.destination
                assertTrue(rect.left.isFinite() && rect.top.isFinite())
                assertTrue(rect.right.isFinite() && rect.bottom.isFinite())
                assertTrue(rect.width >= 0f && rect.height >= 0f)
                assertTrue(rect.left >= viewport.left && rect.right <= viewport.right)
            }
        }
    }

    @Test
    fun envelope_moves_monotonically_toward_the_card() {
        val widths = (0..20).map { step ->
            calculateGenieBands(viewport, card, step / 20f, 20).envelope.width
        }
        assertTrue(widths.zipWithNext().all { (before, after) -> after <= before })
    }
}
```

- [ ] **Step 2: Run the tests and confirm the missing-symbol failure**

Run: `./gradlew :composeApp:allTests --tests '*GenieGeometryTest'`

Expected: FAIL because `calculateGenieBands` is unresolved.

- [ ] **Step 3: Implement a bounded allocation-free geometry API**

Create immutable public results for tests and an overload that fills a reusable destination array for the renderer:

```kotlin
package ge.yet.game.screen.root.transition

import androidx.compose.ui.geometry.Rect
import kotlin.math.pow

internal data class GenieBand(val source: Rect, val destination: Rect)
internal data class GenieGeometry(val envelope: Rect, val bands: List<GenieBand>)

internal fun calculateGenieBands(
    viewport: Rect,
    card: Rect,
    progress: Float,
    bandCount: Int,
): GenieGeometry {
    require(bandCount > 0)
    val p = progress.coerceIn(0f, 1f)
    val envelope = lerpRect(viewport, card, p)
    val bands = List(bandCount) { index ->
        val topFraction = index.toFloat() / bandCount
        val bottomFraction = (index + 1f) / bandCount
        val source = Rect(
            viewport.left,
            viewport.top + viewport.height * topFraction,
            viewport.right,
            viewport.top + viewport.height * bottomFraction,
        )
        val local = (index + .5f) / bandCount
        val delayed = ((p - local * .22f) / (1f - local * .22f)).coerceIn(0f, 1f)
        val funnel = delayed.pow(1.7f)
        val base = lerpRect(viewport, card, p)
        val cardCenter = card.center.x
        val halfWidth = (viewport.width / 2f) + (card.width / 2f - viewport.width / 2f) * funnel
        val center = viewport.center.x + (cardCenter - viewport.center.x) * funnel
        val top = base.top + base.height * topFraction
        val bottom = if (index == bandCount - 1) base.bottom else base.top + base.height * bottomFraction
        GenieBand(source, Rect(center - halfWidth, top, center + halfWidth, bottom))
    }
    return GenieGeometry(envelope, bands)
}

private fun lerpRect(start: Rect, end: Rect, fraction: Float): Rect = Rect(
    left = start.left + (end.left - start.left) * fraction,
    top = start.top + (end.top - start.top) * fraction,
    right = start.right + (end.right - start.right) * fraction,
    bottom = start.bottom + (end.bottom - start.bottom) * fraction,
)
```

The formula is accepted only when the endpoint tests pass exactly. Task 8 replaces its renderer-side list allocation with stable primitive storage without changing these results.

- [ ] **Step 4: Run the focused tests**

Run: `./gradlew :composeApp:allTests --tests '*GenieGeometryTest'`

Expected: PASS.

- [ ] **Step 5: Commit the geometry seam**

```bash
git add composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition/GenieGeometry.kt composeApp/src/commonTest/kotlin/ge/yet/game/screen/root/transition/GenieGeometryTest.kt
git commit -m "feat: add bounded genie transition geometry"
```

## Task 2: Transition State Machine

**Files:**
- Create: `composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition/GenieTransitionState.kt`
- Test: `composeApp/src/commonTest/kotlin/ge/yet/game/screen/root/transition/GenieTransitionStateTest.kt`

- [ ] **Step 1: Write state tests**

Cover idle → expanding → idle, idle → contracting → idle, duplicate rejection, cancellation, and reduced fallback:

```kotlin
@Test
fun duplicate_request_is_rejected_until_current_transition_finishes() {
    val target = GenieTarget(MiniAppId("game.alpha"), Rect(10f, 20f, 210f, 120f))
    val first = GenieTransitionState.Idle.beginExpand(target, reducedMotion = false)
    assertIs<GenieTransitionState.Expanding>(first)
    assertEquals(first, first.beginExpand(target, reducedMotion = false))
}

@Test
fun cancelled_contraction_restores_running_state() {
    val target = GenieTarget(MiniAppId("game.alpha"), Rect(10f, 20f, 210f, 120f))
    val state = GenieTransitionState.Contracting(target, GenieRenderMode.Full)
    assertEquals(GenieTransitionState.Idle, state.cancel())
}
```

- [ ] **Step 2: Verify the tests fail**

Run: `./gradlew :composeApp:allTests --tests '*GenieTransitionStateTest'`

Expected: FAIL because the state types are absent.

- [ ] **Step 3: Implement explicit states**

```kotlin
internal data class GenieTarget(val id: MiniAppId, val boundsInWindow: Rect)

internal enum class GenieRenderMode { Full, Reduced }

internal sealed interface GenieTransitionState {
    val blocksInput: Boolean

    data object Idle : GenieTransitionState { override val blocksInput = false }
    data class Expanding(val target: GenieTarget, val mode: GenieRenderMode) : GenieTransitionState {
        override val blocksInput = true
    }
    data class Contracting(val target: GenieTarget, val mode: GenieRenderMode) : GenieTransitionState {
        override val blocksInput = true
    }

    fun beginExpand(target: GenieTarget, reducedMotion: Boolean): GenieTransitionState =
        if (this != Idle) this else Expanding(target, reducedMotion.toMode())

    fun beginContract(target: GenieTarget, reducedMotion: Boolean): GenieTransitionState =
        if (this != Idle) this else Contracting(target, reducedMotion.toMode())

    fun finish(): GenieTransitionState = Idle
    fun cancel(): GenieTransitionState = Idle
}

private fun Boolean.toMode() = if (this) GenieRenderMode.Reduced else GenieRenderMode.Full
```

- [ ] **Step 4: Run and commit**

Run: `./gradlew :composeApp:allTests --tests '*GenieTransitionStateTest'`

Expected: PASS.

```bash
git add composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition
git commit -m "feat: model genie transition states"
```

## Task 3: Report the Exact Catalog Card Target

**Files:**
- Modify: `feature/catalog/src/commonMain/kotlin/ge/yet/game/feature/catalog/ui/MiniAppListItemCard.kt:24-68`
- Modify: `feature/catalog/src/commonMain/kotlin/ge/yet/game/feature/catalog/ui/CatalogContent.kt:46-179`
- Modify: `feature/catalog/src/commonTest/kotlin/ge/yet/game/feature/catalog/ui/CatalogContentTest.kt`

- [ ] **Step 1: Add a failing UI test for measured launch data**

Render one manifest, click `catalog_play_<id>`, and assert that the presentation override receives the ID and non-empty finite bounds:

```kotlin
var launched: Pair<MiniAppId, Rect>? = null
setContent {
    CatalogContent(
        component = component,
        onPlayFromBounds = { id, bounds -> launched = id to bounds },
    )
}
onNodeWithTag("catalog_play_${manifest.id.value}").performClick()
runOnIdle {
    assertEquals(manifest.id, launched?.first)
    assertTrue(requireNotNull(launched).second.width > 0f)
    assertTrue(requireNotNull(launched).second.height > 0f)
}
```

- [ ] **Step 2: Verify the test fails on the missing parameter**

Run: `./gradlew :feature:catalog:allTests --tests '*CatalogContentTest*reports*'`

Expected: FAIL because `onPlayFromBounds` does not exist.

- [ ] **Step 3: Add the presentation callback without changing `CatalogComponent`**

Use this API shape:

```kotlin
@Composable
fun CatalogContent(
    component: CatalogComponent,
    modifier: Modifier = Modifier,
    onPlayFromBounds: ((MiniAppId, Rect) -> Unit)? = null,
)
```

In `MiniAppListItemCard`, add the state declaration and replace the existing Card modifier with the measured modifier below; leave its existing colors, elevation and `ListItem` body unchanged:

```kotlin
var boundsInWindow by remember(manifest.id) { mutableStateOf(Rect.Zero) }
val measuredModifier = modifier
    .onGloballyPositioned { boundsInWindow = it.boundsInWindow() }
    .testTag("catalog_card_${manifest.id.value}")
```

Pass `measuredModifier` to the existing `Card(modifier = ...)`; no other Card argument changes.

Thread `(MiniAppId, Rect) -> Unit` through `CatalogScreen` and `CatalogGrid`. The button callback must use the measured card rectangle and fall back to the existing component call when no override is installed:

```kotlin
onPlay = {
    if (onPlayFromBounds != null) onPlayFromBounds(manifest.id, boundsInWindow)
    else onPlay(manifest.id)
}
```

- [ ] **Step 4: Run catalog tests and compile both targets**

Run: `./gradlew :feature:catalog:allTests :feature:catalog:compileAndroidMain :feature:catalog:compileKotlinIosSimulatorArm64`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add feature/catalog/src/commonMain/kotlin/ge/yet/game/feature/catalog/ui feature/catalog/src/commonTest/kotlin/ge/yet/game/feature/catalog/ui/CatalogContentTest.kt
git commit -m "feat: report catalog card launch bounds"
```

## Task 4: Add Two-Phase MiniApp Back to Root

**Files:**
- Modify: `feature/root/src/commonMain/kotlin/ge/yet/game/feature/root/RootComponent.kt:13-39`
- Modify: `feature/root/src/commonMain/kotlin/ge/yet/game/feature/root/DefaultRootComponent.kt:109-150`
- Modify: `feature/root/src/commonTest/kotlin/ge/yet/game/feature/root/DefaultRootComponentTest.kt:208-235`

- [ ] **Step 1: Write lifecycle-order tests**

Add tests proving that preparing Back calls `session.handleBack()` once, does not destroy an unconsumed session, and that only a matching commit closes it:

```kotlin
@Test
fun deferred_back_closes_only_after_matching_commit() {
    val setup = build()
    setup.lifecycle.resume()
    setup.play(FIRST_ID)

    val decision = setup.component.prepareMiniAppBack()
    val close = assertIs<RootComponent.MiniAppBackDecision.AnimateClose>(decision)
    assertIs<RootComponent.Child.RunningMiniApp>(setup.component.stack.value.active.instance)
    assertEquals(0, setup.firstPlugin.destroyCount)

    setup.component.commitMiniAppBack(close.token)

    assertIs<RootComponent.Child.Catalog>(setup.component.stack.value.active.instance)
    assertEquals(1, setup.firstPlugin.destroyCount)
}

@Test
fun stale_back_token_cannot_close_a_new_session() {
    val setup = build()
    setup.play(FIRST_ID)
    val token = assertIs<RootComponent.MiniAppBackDecision.AnimateClose>(
        setup.component.prepareMiniAppBack(),
    ).token
    setup.component.cancelMiniAppBack(token)
    setup.component.onBackClicked()
    setup.play(SECOND_ID)
    setup.component.commitMiniAppBack(token)
    assertEquals(SECOND_ID, setup.running().id)
}
```

- [ ] **Step 2: Verify failure**

Run: `./gradlew :feature:root:allTests --tests '*DefaultRootComponentTest*deferred*' --tests '*DefaultRootComponentTest*stale_back_token*'`

Expected: FAIL because the two-phase contract is absent.

- [ ] **Step 3: Add the contract and token validation**

Add to `RootComponent`:

```kotlin
sealed interface MiniAppBackDecision {
    data object Consumed : MiniAppBackDecision
    data object NotRunning : MiniAppBackDecision
    data class AnimateClose(val token: Long, val id: MiniAppId) : MiniAppBackDecision
}

fun prepareMiniAppBack(): MiniAppBackDecision
fun commitMiniAppBack(token: Long)
fun cancelMiniAppBack(token: Long)
```

In `DefaultRootComponent`, keep one pending token paired with the active runtime key or ID. `prepareMiniAppBack()` must call `handleBack()` exactly once. `commitMiniAppBack()` calls `runtimeCoordinator.closeActiveSession()` only when the pending token and currently active ID match. `cancelMiniAppBack()` only clears the matching token. Existing `onBackClicked()` stays as the immediate non-animated fallback for hosts with no Compose transition coordinator.

Use monotonically increasing `Long` tokens local to the component; do not persist them.

- [ ] **Step 4: Run the full Root component suite**

Run: `./gradlew :feature:root:allTests`

Expected: BUILD SUCCESSFUL, including the pre-existing immediate Back tests.

- [ ] **Step 5: Commit**

```bash
git add feature/root/src/commonMain/kotlin/ge/yet/game/feature/root feature/root/src/commonTest/kotlin/ge/yet/game/feature/root/DefaultRootComponentTest.kt
git commit -m "feat: defer miniapp close for visual transitions"
```

## Task 5: Implement the Compose Capture and Canvas Renderer

**Files:**
- Create: `composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition/GenieTransitionCoordinator.kt`
- Create: `composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition/GenieTransitionOverlay.kt`
- Test: `composeApp/src/commonTest/kotlin/ge/yet/game/screen/root/transition/GenieTransitionOverlayTest.kt`

- [ ] **Step 1: Add renderer semantics and fallback tests**

Test that the overlay has `clearAndSetSemantics { }`, blocks pointer input, exposes a deterministic test tag, and selects reduced mode when capture is absent:

```kotlin
onNodeWithTag("genie_transition_overlay").assertExists()
onNodeWithTag("genie_transition_overlay", useUnmergedTree = true)
    .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
```

Also test the pure helper:

```kotlin
assertEquals(GenieRenderMode.Reduced, resolveRenderMode(false, hasCapture = false))
assertEquals(GenieRenderMode.Reduced, resolveRenderMode(true, hasCapture = true))
assertEquals(GenieRenderMode.Full, resolveRenderMode(false, hasCapture = true))
```

- [ ] **Step 2: Run the focused test and confirm failure**

Run: `./gradlew :composeApp:allTests --tests '*GenieTransitionOverlayTest'`

Expected: FAIL because the renderer is missing.

- [ ] **Step 3: Implement one-shot layer recording**

Use Compose 1.12's common graphics-layer APIs behind a focused composable. Record the live source while it is laid out, then call `toImageBitmap()` once from the coordinator before animation. The source must never be captured from `drawWithCache` on every invalidation.

The coordinator owns one `Animatable<Float, AnimationVector1D>` and these exact specs:

```kotlin
internal const val GENIE_EXPAND_MS = 440
internal const val GENIE_CONTRACT_MS = 380
internal const val GENIE_REDUCED_MS = 160
internal const val GENIE_BAND_COUNT = 20

internal val GenieEasing = CubicBezierEasing(.22f, .72f, .18f, 1f)
```

Expansion animates `0f → 1f`; contraction animates `0f → 1f` but renders geometry at `1f - progress`. Cancellation animates back to the running endpoint before clearing state.

- [ ] **Step 4: Implement Canvas drawing with stable work buffers**

`GenieTransitionOverlay` receives only an `ImageBitmap`, viewport, target, direction and scalar progress. Build source rectangles when the bitmap changes. Reuse destination storage and draw exactly 20 slices. Do not allocate `List`, `Rect`, `Path`, or lambdas inside the per-band loop after the optimization is installed.

Draw the affine reduced fallback with one `graphicsLayer` scale/translation and alpha; never attempt strip drawing without a valid captured image.

- [ ] **Step 5: Run tests and target compilation**

Run: `./gradlew :composeApp:allTests --tests '*GenieTransition*' :composeApp:compileAndroidMain :composeApp:compileKotlinIosSimulatorArm64`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition composeApp/src/commonTest/kotlin/ge/yet/game/screen/root/transition
git commit -m "feat: render compose genie transition overlay"
```

## Task 6: Integrate Catalog Launch and Deferred Back in `RootContent`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition/RootGenieContent.kt`
- Modify: `composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/RootContent.kt:19-88`
- Modify: `composeApp/src/commonTest/kotlin/ge/yet/game/screen/root/RootContentTest.kt`

- [ ] **Step 1: Add failing launch/back integration tests**

Build a mutable fake Root stack and assert:

1. Play records the selected card target before `onPlayClicked` changes the stack.
2. A second Play is ignored while expanding.
3. Back calls `prepareMiniAppBack`, leaves Running active during contraction, then calls `commitMiniAppBack` after the 380 ms clock advance.

Use the manual test clock:

```kotlin
mainClock.autoAdvance = false
onNodeWithTag("miniapp_back_control").performClick()
runOnIdle { assertEquals(1, component.prepareBackCalls) }
assertEquals(0, component.commitBackCalls)
mainClock.advanceTimeBy(GENIE_CONTRACT_MS.toLong() + 32L)
runOnIdle { assertEquals(1, component.commitBackCalls) }
```

- [ ] **Step 2: Verify failure**

Run: `./gradlew :composeApp:allTests --tests '*RootContentTest*genie*'`

Expected: FAIL because `RootContent` still installs `cupertinoPredictiveBackAnimation` directly.

- [ ] **Step 3: Install a Root-only transition host**

Move the Catalog/Running visual coordination into `RootGenieContent`. Keep `RootChildContent` focused on rendering one child. The integration must:

- retain the last `GenieTarget` by `MiniAppId`;
- call `CatalogContent(onPlayFromBounds = ...)`;
- invoke the existing `CatalogComponent.onPlayClicked(id)` exactly once after recording the target;
- wait for valid Running layout/capture before starting full expansion;
- route toolbar Back through `prepareMiniAppBack()`;
- call `commitMiniAppBack(token)` only on completed contraction;
- call `cancelMiniAppBack(token)` on cancellation/disposal;
- place the catalog under the transition overlay and expose only one semantics tree;
- use a full-size pointer-consuming overlay whenever `state.blocksInput` is true.

Do not move MiniApp frame, background, toolbar, banner, or theme ownership out of `RootChildContent`.

- [ ] **Step 4: Preserve non-Catalog/Running animation behavior**

Retain `cupertinoPredictiveBackAnimation` as the fallback selector for any Root stack pair not recognized by `RootGenieContent`. Do not change the Settings `Children(stackAnimation(slide()))` call.

- [ ] **Step 5: Run Root/Compose tests**

Run: `./gradlew :composeApp:allTests --tests '*RootContentTest' :feature:root:allTests :feature:catalog:allTests`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/ge/yet/game/screen/root composeApp/src/commonTest/kotlin/ge/yet/game/screen/root/RootContentTest.kt
git commit -m "feat: connect catalog and miniapp genie transitions"
```

## Task 7: Reduced Motion and Predictive Back

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition/GenieTransitionCoordinator.kt`
- Modify: `composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition/RootGenieContent.kt`
- Modify: `composeApp/src/commonMain/kotlin/ge/yet/game/utils/CupertinoPredictiveBack.kt`
- Modify: `composeApp/src/commonTest/kotlin/ge/yet/game/screen/root/RootContentTest.kt`

- [ ] **Step 1: Add failing reduced-motion and predictive-cancel tests**

Provide motion scale as an injectable value to the internal transition host so tests do not mutate platform globals:

```kotlin
RootGenieContent(component = component, motionDurationScale = 0f)
```

Assert the transition finishes after 160 ms, uses the reduced tag/state, and never installs band rendering. Add a predictive Back test that advances progress, cancels, and verifies `cancelMiniAppBack(token)` is called while `commitMiniAppBack` is not.

- [ ] **Step 2: Verify the new tests fail**

Run: `./gradlew :composeApp:allTests --tests '*RootContentTest*reduced*' --tests '*RootContentTest*predictive*'`

Expected: FAIL because duration-scale and gesture progress are not connected.

- [ ] **Step 3: Connect Compose motion scale**

Read `LocalMotionDurationScale.current.scaleFactor` once above the transition host. Treat `scaleFactor == 0f` as reduced motion. For positive scale factors, keep the specified wall-clock animation semantics used by Compose's animation system; do not multiply frame counts manually.

- [ ] **Step 4: Adapt the existing safe predictive-back seam**

Extract a Root-only predictive callback/animatable from `CupertinoPredictiveBack.kt` that forwards normalized progress, commit and cancel to the coordinator while retaining the existing translation-only implementation for Settings/other stack transitions. A cancel must animate to the Running endpoint, clear the token through `cancelMiniAppBack`, and restore input.

- [ ] **Step 5: Run all affected tests and compile targets**

Run: `./gradlew :composeApp:allTests :feature:root:allTests :feature:catalog:allTests :composeApp:compileAndroidMain :composeApp:compileKotlinIosSimulatorArm64`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/ge/yet/game/screen/root composeApp/src/commonMain/kotlin/ge/yet/game/utils/CupertinoPredictiveBack.kt composeApp/src/commonTest/kotlin/ge/yet/game/screen/root/RootContentTest.kt
git commit -m "feat: support reduced and predictive genie motion"
```

## Task 8: Performance Tightening and End-to-End Verification

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition/GenieGeometry.kt`
- Modify: `composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition/GenieTransitionOverlay.kt`
- Modify: `composeApp/src/commonTest/kotlin/ge/yet/game/screen/root/transition/GenieGeometryTest.kt`
- Modify if architecture or standard commands change: `AGENTS.md`

- [ ] **Step 1: Add a reusable-buffer regression test**

Expose an internal `GenieBandBuffer(20)` whose `update(viewport, card, progress)` mutates primitive float arrays. Test that repeated updates return the same buffer identity and keep finite endpoints:

```kotlin
val buffer = GenieBandBuffer(20)
val first = buffer.update(viewport, card, 0f)
repeat(1_000) { buffer.update(viewport, card, (it % 101) / 100f) }
buffer.update(viewport, card, 1f)
assertSame(first, buffer)
assertEquals(card, buffer.envelope)
```

- [ ] **Step 2: Replace per-frame geometry allocations**

Keep the immutable `calculateGenieBands` helper for unit readability, but make the renderer use `GenieBandBuffer`. Precompute all source slice coordinates when the bitmap or viewport changes. Each frame reads progress once and performs a fixed indexed loop over primitive arrays.

- [ ] **Step 3: Run formatting/static checks and focused suites**

Run:

```bash
./gradlew :feature:catalog:allTests
./gradlew :feature:root:allTests
./gradlew :composeApp:allTests
./gradlew :composeApp:compileAndroidMain
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

Expected: every command reports BUILD SUCCESSFUL.

- [ ] **Step 4: Package Android and link the iOS framework**

Run: `./gradlew :androidApp:assembleDebug :composeApp:linkDebugFrameworkIosSimulatorArm64`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Perform manual motion validation**

On available 60 Hz and 120 Hz Android targets, verify launch and Back for cards in each catalog column and after scrolling. Record with system frame-timeline tooling and confirm:

- one capture occurs before each transition and none during it;
- duration remains 440/380 ms independent of refresh rate;
- no visible first-frame flash or final-frame jump;
- the overlay enters the measured card bounds;
- repeated input is ignored;
- reduced motion finishes in 160 ms;
- cancelled predictive Back restores the live Running child.

If a 120 Hz physical target or unlocked iOS simulator is unavailable, record that limitation in the final handoff rather than claiming it was verified.

- [ ] **Step 6: Review architecture documentation**

If implementation changes only the existing Root presentation transition, `AGENTS.md` needs no edit. If the implementation adds a new cross-module contract or changes Root navigation ownership, update the relevant Module Responsibilities and MiniApp host-ownership paragraphs before committing.

- [ ] **Step 7: Commit the verified implementation**

```bash
git add composeApp/src/commonMain/kotlin/ge/yet/game/screen/root/transition composeApp/src/commonTest/kotlin/ge/yet/game/screen/root/transition AGENTS.md
git commit -m "perf: bound genie transition frame work"
```

## Final Review Checklist

- [ ] Every design-spec requirement maps to Tasks 1–8.
- [ ] Catalog bounds remain presentation-only.
- [ ] The MiniApp session receives Back once per gesture/click.
- [ ] Session teardown occurs only after committed contraction.
- [ ] Cancelled predictive Back retains the existing session.
- [ ] No capture or collection allocation occurs in the render loop.
- [ ] Reduced motion, invalid bounds and failed capture cannot stall navigation.
- [ ] Exact commands and any unavailable device checks are reported in the handoff.
