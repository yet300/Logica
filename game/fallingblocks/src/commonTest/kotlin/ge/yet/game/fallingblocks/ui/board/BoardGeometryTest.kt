package ge.yet.game.fallingblocks.ui.board

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardGeometryTest {
    @Test
    fun `portrait board is centered and keeps ten by twenty aspect`() {
        val geometry = BoardGeometry.fit(
            viewportWidth = 400f,
            viewportHeight = 800f,
            edgeInset = 16f,
        )

        assertEquals(200f, geometry.centerX)
        assertEquals(400f, geometry.centerY)
        assertEquals(0.5f, geometry.width / geometry.height)
        assertEquals(36.8f, geometry.cellSize, absoluteTolerance = 0.001f)
    }

    @Test
    fun `wide viewport centers board independently of supporting overlays`() {
        val geometry = BoardGeometry.fit(
            viewportWidth = 1_200f,
            viewportHeight = 700f,
            edgeInset = 24f,
        )

        assertEquals(600f, geometry.centerX)
        assertEquals(350f, geometry.centerY)
        assertEquals(326f, geometry.width)
        assertEquals(652f, geometry.height)
    }

    @Test
    fun `tiny viewport remains finite and inside its bounds`() {
        val geometry = BoardGeometry.fit(20f, 30f, edgeInset = 40f)

        assertTrue(geometry.width > 0f)
        assertTrue(geometry.height > 0f)
        assertTrue(geometry.left >= 0f)
        assertTrue(geometry.top >= 0f)
        assertTrue(geometry.right <= 20f)
        assertTrue(geometry.bottom <= 30f)
    }
}
