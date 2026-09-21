package ge.yet.game.fallingblocks.ui.board

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardGeometryTest {
    @Test
    fun `phone board reserves preview space and keeps ten by twenty aspect`() {
        val geometry = BoardGeometry.fit(400f, 800f, 24f, 64f, 340f)

        assertEquals(200f, geometry.centerX)
        assertEquals(400f, geometry.centerY)
        assertEquals(336f, geometry.width, absoluteTolerance = 0.01f)
        assertEquals(0.5f, geometry.width / geometry.height)
    }

    @Test
    fun `compact phone is height limited after preview reserve`() {
        val geometry = BoardGeometry.fit(360f, 640f, 24f, 48f, 340f)

        assertEquals(272f, geometry.width, absoluteTolerance = 0.01f)
        assertEquals(180f, geometry.centerX, absoluteTolerance = 0.01f)
        assertEquals(320f, geometry.centerY, absoluteTolerance = 0.01f)
    }

    @Test
    fun `wide viewport remains capped and centered`() {
        val geometry = BoardGeometry.fit(1_200f, 700f, 24f, 64f, 340f)

        assertEquals(600f, geometry.centerX)
        assertEquals(350f, geometry.centerY)
        assertEquals(294f, geometry.width)
        assertEquals(588f, geometry.height)
        assertTrue(geometry.width <= 340f)
    }

    @Test
    fun `tiny viewport remains finite and inside its bounds`() {
        val geometry = BoardGeometry.fit(20f, 30f, 40f, 48f, 340f)

        assertTrue(geometry.width > 0f)
        assertTrue(geometry.height > 0f)
        assertTrue(geometry.left >= 0f)
        assertTrue(geometry.top >= 0f)
        assertTrue(geometry.right <= 20f)
        assertTrue(geometry.bottom <= 30f)
        assertEquals(0.5f, geometry.width / geometry.height)
    }
}
