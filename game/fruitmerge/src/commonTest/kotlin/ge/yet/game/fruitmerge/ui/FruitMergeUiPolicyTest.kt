package ge.yet.game.fruitmerge.ui

import ge.yet.game.fruitmerge.domain.engine.FruitMergeEngine
import ge.yet.game.fruitmerge.domain.model.FruitBody
import ge.yet.game.fruitmerge.domain.model.FruitLevel
import ge.yet.game.fruitmerge.domain.model.Vec2
import ge.yet.game.uikit.components.score.ScoreCardState
import ge.yet.game.uikit.components.score.scoreCardState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.math.abs

class FruitMergeUiPolicyTest {
    @Test
    fun `new record presents one crowned value like 2048`() {
        assertEquals(ScoreCardState.BestOnly, scoreCardState(bestScore = 1_200, bestImprovedInRun = true))
        assertEquals(ScoreCardState.ScoreAndBest, scoreCardState(bestScore = 1_200, bestImprovedInRun = false))
        assertEquals(ScoreCardState.ScoreOnly, scoreCardState(bestScore = 0, bestImprovedInRun = false))
    }
    @Test
    fun `active shake uses a smaller transform for reduced motion`() {
        val active = shakeVisualTransform(FruitMergeEngine.SHAKE_DURATION_STEPS - 1, false)
        val reduced = shakeVisualTransform(FruitMergeEngine.SHAKE_DURATION_STEPS - 1, true)
        val idle = shakeVisualTransform(0, false)

        assertNotEquals(0f, active.translationXDp)
        assertNotEquals(0f, reduced.translationXDp)
        assertTrue(abs(reduced.translationXDp) < abs(active.translationXDp))
        assertTrue(abs(reduced.rotationDegrees) < abs(active.rotationDegrees))
        assertEquals(ShakeVisualTransform(0f, 0f), idle)
    }

    @Test
    fun `merge squeeze is bounded to a short event and returns to rest`() {
        assertEquals(0f, mergePresentationProgress(-0.1f))
        assertEquals(1f, mergePresentationProgress(0.15f))
        assertEquals(MergeSqueeze(1f, 1f), mergeSqueeze(0f))
        assertEquals(MergeSqueeze(1f, 1f), mergeSqueeze(1f))
        val middle = mergeSqueeze(0.5f)
        assertTrue(middle.scaleX > 1f)
        assertTrue(middle.scaleY < 1f)
    }

    @Test
    fun `visual tilt clamps physical spin to a gentle wobble`() {
        assertEquals(0f, visualTiltDegrees(0f))
        assertEquals(0f, visualTiltDegrees(Float.NaN))
        assertTrue(abs(visualTiltDegrees(10f)) <= MAX_VISUAL_TILT_DEGREES)
        assertTrue(abs(visualTiltDegrees(-10f)) <= MAX_VISUAL_TILT_DEGREES)
        // Small angles pass through unwrapped.
        assertTrue(abs(visualTiltDegrees(0.2f) - 0.2f * (180f / kotlin.math.PI.toFloat())) < 0.001f)
        // Full turns wrap back to rest instead of snapping.
        assertTrue(abs(visualTiltDegrees(2f * kotlin.math.PI.toFloat())) < 0.001f)
    }

    @Test
    fun `face only leans a little with the body tilt`() {
        assertEquals(0f, faceTiltDegrees(0f))
        assertEquals(
            MAX_VISUAL_TILT_DEGREES * FACE_TILT_FOLLOW,
            faceTiltDegrees(MAX_VISUAL_TILT_DEGREES),
        )
        assertTrue(abs(faceTiltDegrees(100f)) <= MAX_FACE_TILT_DEGREES)
        assertTrue(abs(faceTiltDegrees(-100f)) <= MAX_FACE_TILT_DEGREES)
    }

    @Test
    fun `small fruits draw after big ones so they stay visible`() {
        val bodies = listOf(
            FruitBody(id = 1, level = FruitLevel.BLUEBERRY, position = Vec2(0.5f, 0.5f)),
            FruitBody(id = 2, level = FruitLevel.PINEAPPLE, position = Vec2(0.5f, 0.7f)),
            FruitBody(id = 3, level = FruitLevel.WATERMELON, position = Vec2(0.5f, 0.8f)),
        )

        assertEquals(listOf(3L, 2L, 1L), fruitDrawOrder(bodies).map { it.id })
    }

    @Test
    fun `face clock wrap is a multiple of every blink interval`() {
        assertEquals((FACE_CLOCK_WRAP_SECONDS * 1000).toInt(), FACE_CLOCK_PERIOD_MILLIS)
        val defaultRemainder = FACE_CLOCK_WRAP_SECONDS % DEFAULT_BLINK_INTERVAL_SECONDS
        val calmRemainder = FACE_CLOCK_WRAP_SECONDS % CALM_BLINK_INTERVAL_SECONDS
        assertTrue(defaultRemainder < 0.001f, "Wrap must not jump the default blink phase")
        assertTrue(calmRemainder < 0.001f, "Wrap must not jump the calm blink phase")
    }

    @Test
    fun `guide fades during cooldown and handle follows shared shake phase`() {
        assertEquals(1f, guideAlpha(0f))
        assertEquals(0f, guideAlpha(0.25f))
        val steps = FruitMergeEngine.SHAKE_DURATION_STEPS - 1
        assertEquals(shakeVisualTransform(steps, false).rotationDegrees * 7f, crateHandleRotation(steps, false))
    }
}
