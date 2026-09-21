package ge.yet.game.fallingblocks.ui.motion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FallingBlocksMotionPolicyTest {
    @Test
    fun `normal motion keeps spatial feedback`() {
        val policy = fallingBlocksMotionPolicy(reducedMotion = false)

        assertTrue(policy.spatialMotionEnabled)
        assertEquals(70, policy.moveDurationMillis)
        assertEquals(280, policy.gameOverDurationMillis)
        assertEquals(90, policy.hardDropTrailDurationMillis)
        assertEquals(140, policy.hardDropImpactDurationMillis)
        assertEquals(180, policy.lineClearFlashDurationMillis)
        assertEquals(160, policy.lineClearCollapseDurationMillis)
    }

    @Test
    fun `reduced motion removes translation and keeps short opacity feedback`() {
        val policy = fallingBlocksMotionPolicy(reducedMotion = true)

        assertFalse(policy.spatialMotionEnabled)
        assertEquals(0, policy.moveDurationMillis)
        assertEquals(72, policy.opacityDurationMillis)
        assertEquals(0, policy.hardDropTrailDurationMillis)
        assertEquals(80, policy.hardDropImpactDurationMillis)
        assertEquals(90, policy.lineClearFlashDurationMillis)
        assertEquals(0, policy.lineClearCollapseDurationMillis)
    }
}
