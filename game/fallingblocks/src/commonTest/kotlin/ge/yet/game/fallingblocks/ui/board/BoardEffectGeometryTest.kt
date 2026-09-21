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
    fun `hard drop trails clip hidden origins into normalized board space`() {
        val trails = hardDropTrails(
            from = listOf(Cell(3, 0), Cell(4, Board.HIDDEN_ROWS + 2)),
            to = listOf(Cell(3, Board.TOTAL_HEIGHT - 1), Cell(4, Board.TOTAL_HEIGHT - 2)),
        )

        assertEquals(2, trails.size)
        assertTrue(trails.all { it.centerX in 0f..1f })
        assertTrue(trails.all { it.fromY in 0f..1f && it.toY in 0f..1f })
        assertEquals(0.025f, trails.first().fromY, absoluteTolerance = 0.0001f)
    }
}
