package ge.yet.game.fruitmerge.ui

import ge.yet.game.fruitmerge.domain.model.FruitLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FruitVisualSpecTest {
    @Test
    fun `every level has a distinct silhouette detail and face identity`() {
        val identities = FruitLevel.entries.map { fruitVisualSpec(it).identityKey }

        assertEquals(FruitLevel.entries.size, identities.toSet().size)
        assertNotEquals(
            fruitVisualSpec(FruitLevel.BLUEBERRY).silhouette,
            fruitVisualSpec(FruitLevel.RASPBERRY).silhouette,
        )
        assertNotEquals(
            fruitVisualSpec(FruitLevel.RASPBERRY).silhouette,
            fruitVisualSpec(FruitLevel.STRAWBERRY).silhouette,
        )
        assertEquals(
            FruitSilhouette.CLUSTER,
            fruitVisualSpec(FruitLevel.RASPBERRY).silhouette,
        )
        assertEquals(
            FruitDetail.DRUPELETS,
            fruitVisualSpec(FruitLevel.RASPBERRY).detail,
        )
        assertEquals(
            FruitSilhouette.CITRUS,
            fruitVisualSpec(FruitLevel.LIME).silhouette,
        )
        assertEquals(
            FruitDetail.WEDGES,
            fruitVisualSpec(FruitLevel.LIME).detail,
        )
        assertEquals(
            FruitSilhouette.CROWNED,
            fruitVisualSpec(FruitLevel.PINEAPPLE).silhouette,
        )
        assertEquals(
            FruitDetail.DIAMONDS,
            fruitVisualSpec(FruitLevel.PINEAPPLE).detail,
        )
        assertEquals(
            FruitSilhouette.STRIPED,
            fruitVisualSpec(FruitLevel.WATERMELON).silhouette,
        )
        assertEquals(
            FruitDetail.RIND,
            fruitVisualSpec(FruitLevel.WATERMELON).detail,
        )
    }

    @Test
    fun `all fruits share one outline and upper left light with a bounded three tone palette`() {
        val specs = FruitLevel.entries.map(::fruitVisualSpec)

        assertEquals(1, specs.map { it.outline }.distinct().size)
        assertTrue(specs.all { it.lightDirection == FruitLightDirection.UPPER_LEFT })
        assertTrue(specs.all { it.base != it.shadow && it.base != it.highlight && it.shadow != it.highlight })
    }

    @Test
    fun `peach and pineapple blink calmly twice as rarely`() {
        val peach = fruitVisualSpec(FruitLevel.PEACH)
        val pineapple = fruitVisualSpec(FruitLevel.PINEAPPLE)
        val blueberry = fruitVisualSpec(FruitLevel.BLUEBERRY)

        assertEquals(CALM_BLINK_INTERVAL_SECONDS, peach.blinkIntervalSeconds)
        assertEquals(CALM_BLINK_INTERVAL_SECONDS, pineapple.blinkIntervalSeconds)
        assertEquals(DEFAULT_BLINK_INTERVAL_SECONDS, blueberry.blinkIntervalSeconds)

        val calmDuty = CALM_BLINK_CLOSED_SECONDS / CALM_BLINK_INTERVAL_SECONDS
        val defaultDuty = DEFAULT_BLINK_CLOSED_SECONDS / DEFAULT_BLINK_INTERVAL_SECONDS
        assertTrue(calmDuty < defaultDuty, "Calm duty $calmDuty must be below default $defaultDuty")
    }

    @Test
    fun `isFruitBlinking respects per-level window and finite input`() {
        // Default 4.2s / 0.14s window.
        assertTrue(isFruitBlinking(FruitLevel.BLUEBERRY, 0f))
        assertTrue(isFruitBlinking(FruitLevel.BLUEBERRY, 0.13f))
        assertTrue(!isFruitBlinking(FruitLevel.BLUEBERRY, 0.15f))
        assertTrue(!isFruitBlinking(FruitLevel.BLUEBERRY, 2.1f))
        // Negative phases wrap instead of blinking constantly.
        assertTrue(!isFruitBlinking(FruitLevel.BLUEBERRY, -1f))
        // Non-finite never blinks.
        assertTrue(!isFruitBlinking(FruitLevel.BLUEBERRY, Float.NaN))

        // Calm 8.4s / 0.20s window: still open at the old default re-blink point.
        assertTrue(!isFruitBlinking(FruitLevel.PEACH, 4.25f))
        assertTrue(!isFruitBlinking(FruitLevel.PINEAPPLE, 4.25f))
        assertTrue(isFruitBlinking(FruitLevel.PEACH, 0.1f))
        assertTrue(!isFruitBlinking(FruitLevel.PEACH, 4.2f))
    }

    @Test
    fun `blink offsets are deterministic bounded and decorrelated`() {
        for (level in FruitLevel.entries) {
            val interval = fruitVisualSpec(level).blinkIntervalSeconds
            val offsets = (1L..32L).map { fruitBlinkOffset(it, level) }
            assertTrue(offsets.all { it >= 0f && it < interval }, "Offsets must stay in [0, interval) for $level")
            assertEquals(offsets, (1L..32L).map { fruitBlinkOffset(it, level) })
            val steps = offsets.zipWithNext { a, b -> kotlin.math.abs(b - a) }
            assertTrue(steps.any { it > 0.01f }, "Neighbour ids must not march as a fixed wave for $level")
        }
    }

    @Test
    fun `resting phase never blinks`() {
        for (level in FruitLevel.entries) {
            assertTrue(!isFruitBlinking(level, fruitRestingPhase(level)), "Resting phase must be open for $level")
        }
    }

    @Test
    fun `all fruits have distinct base colors and defined character blush tones`() {
        val specs = FruitLevel.entries.map(::fruitVisualSpec)

        // Every fruit has an identifiable unique base hue
        assertEquals(FruitLevel.entries.size, specs.map { it.base }.distinct().size)
        // Every fruit has defined blush, face ink, and leaf colors
        assertTrue(specs.all { it.blush.alpha > 0f })
        assertTrue(specs.all { it.faceInk.alpha > 0f })
        assertTrue(specs.all { it.leaf.alpha > 0f })
    }
}
