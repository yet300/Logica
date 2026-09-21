package ge.yet.game.fallingblocks.ui.board

import ge.yet.game.fallingblocks.component.game.VisualCell
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.Tetromino
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoardEffectGeometryTest {
    @Test
    fun `visible coordinates are normalized and hidden rows are clipped`() {
        assertNull(normalizedEffectCell(Cell(0, Board.HIDDEN_ROWS - 1)))

        val topLeft = normalizedEffectCell(Cell(0, Board.HIDDEN_ROWS))!!
        val bottomRight = normalizedEffectCell(
            Cell(Board.WIDTH - 1, Board.TOTAL_HEIGHT - 1),
        )!!

        assertEquals(0f, topLeft.left)
        assertEquals(0f, topLeft.top)
        assertEquals(1f, bottomRight.right)
        assertEquals(1f, bottomRight.bottom)
    }

    @Test
    fun `particles are deterministic normalized and globally bounded`() {
        val cells = (Board.TOTAL_HEIGHT - 4 until Board.TOTAL_HEIGHT).flatMap { y ->
            (0 until Board.WIDTH).map { x -> VisualCell(Cell(x, y), Tetromino.T) }
        }

        val first = lineClearParticles(eventId = 41, cells = cells)
        val repeated = lineClearParticles(eventId = 41, cells = cells)
        val other = lineClearParticles(eventId = 42, cells = cells)

        assertEquals(first, repeated)
        assertNotEquals(first, other)
        assertTrue(first.size <= 96)
        assertTrue(first.size <= cells.size * 8)
        assertTrue(first.all { it.originX in 0f..1f && it.originY in 0f..1f })
        assertTrue(first.all { it.size in 0.006f..0.03f })
    }

    @Test
    fun `shockwaves center on visible cleared rows and skip hidden rows`() {
        val bottom = Board.TOTAL_HEIGHT - 1
        val waves = lineClearShockwaves(listOf(0, Board.HIDDEN_ROWS, bottom))

        assertEquals(2, waves.size)
        waves.forEach { wave ->
            assertEquals(0.5f, wave.centerX)
            assertTrue(wave.centerY in 0f..1f)
        }
        val expectedTop = (Board.HIDDEN_ROWS - Board.HIDDEN_ROWS + 0.5f) / Board.VISIBLE_HEIGHT
        assertEquals(expectedTop, waves[0].centerY, absoluteTolerance = 1e-4f)
        val expectedBottom = (bottom - Board.HIDDEN_ROWS + 0.5f) / Board.VISIBLE_HEIGHT
        assertEquals(expectedBottom, waves[1].centerY, absoluteTolerance = 1e-4f)
    }

    @Test
    fun `row slots follow sorted order for cascade delays`() {
        val rows = listOf(18, 19, 20, 21)
        assertEquals(0, lineClearRowSlot(18, rows))
        assertEquals(3, lineClearRowSlot(21, rows))
        assertEquals(90L, lineClearCascadeDelayMs(1))
        assertEquals(270L, lineClearCascadeDelayMs(3))
    }

}
