package ge.yet.game.uikit.motion

import androidx.compose.ui.MotionDurationScale
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReducedMotionTest {
    @Test
    fun `zero duration scale is reduced motion`() {
        assertTrue(durationScale(0f).isReducedMotion())
        assertFalse(durationScale(1f).isReducedMotion())
        assertFalse(null.isReducedMotion())
    }

    private fun durationScale(scaleFactor: Float): MotionDurationScale =
        object : MotionDurationScale {
            override val scaleFactor: Float = scaleFactor
        }
}
