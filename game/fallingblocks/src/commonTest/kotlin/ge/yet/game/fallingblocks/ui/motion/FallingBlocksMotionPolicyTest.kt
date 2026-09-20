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
    }

    @Test
    fun `reduced motion removes translation and keeps short opacity feedback`() {
        val policy = fallingBlocksMotionPolicy(reducedMotion = true)

        assertFalse(policy.spatialMotionEnabled)
        assertEquals(0, policy.moveDurationMillis)
        assertEquals(72, policy.opacityDurationMillis)
    }
}
