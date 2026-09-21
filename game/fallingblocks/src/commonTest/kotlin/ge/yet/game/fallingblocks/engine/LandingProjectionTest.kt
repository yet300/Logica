package ge.yet.game.fallingblocks.engine

import ge.yet.game.fallingblocks.domain.engine.landingPiece
import ge.yet.game.fallingblocks.domain.model.ActivePiece
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.Rotation
import ge.yet.game.fallingblocks.domain.model.Tetromino
import kotlin.test.Test
import kotlin.test.assertEquals

class LandingProjectionTest {
    @Test
    fun `landing projection stops immediately above settled cells without mutating board`() {
        val cells = Board.empty().cells.toMutableList()
        cells[21 * Board.WIDTH + 4] = Tetromino.J
        val board = Board(cells)
        val piece = ActivePiece(Tetromino.O, Rotation.SPAWN, Cell(4, 3))

        val landed = landingPiece(piece, board)

        assertEquals(Cell(4, 20), landed.origin)
        assertEquals(Tetromino.J, board[Cell(4, 21)])
    }
}
