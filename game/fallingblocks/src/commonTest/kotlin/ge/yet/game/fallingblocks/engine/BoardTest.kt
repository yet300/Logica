package ge.yet.game.fallingblocks.engine

import ge.yet.game.fallingblocks.domain.model.Board
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BoardTest {
    @Test
    fun `board has ten columns twenty visible and two hidden rows`() {
        assertEquals(10, Board.WIDTH)
        assertEquals(20, Board.VISIBLE_HEIGHT)
        assertEquals(2, Board.HIDDEN_ROWS)
        assertEquals(22, Board.TOTAL_HEIGHT)
        assertFailsWith<IllegalArgumentException> { Board(List(219) { null }) }
    }

    @Test
    fun `empty board contains only empty cells`() {
        val board = Board.empty()

        assertEquals(Board.WIDTH * Board.TOTAL_HEIGHT, board.cells.size)
        assertTrue(board.cells.all { it == null })
    }
}
