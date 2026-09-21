package ge.yet.game.fallingblocks.engine

import ge.yet.game.fallingblocks.domain.engine.canOccupy
import ge.yet.game.fallingblocks.domain.engine.cells
import ge.yet.game.fallingblocks.domain.engine.tryRotateClockwise
import ge.yet.game.fallingblocks.domain.model.ActivePiece
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.Rotation
import ge.yet.game.fallingblocks.domain.model.Tetromino
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SrsRotationTest {
    @Test
    fun `every tetromino rotation contains four distinct cells`() {
        Tetromino.entries.forEach { type ->
            Rotation.entries.forEach { rotation ->
                val cells = ActivePiece(type, rotation, Cell(4, 6)).cells()

                assertEquals(4, cells.size, "$type $rotation")
                assertEquals(4, cells.toSet().size, "$type $rotation")
            }
        }
    }

    @Test
    fun `o geometry is invariant across rotations`() {
        val origin = Cell(4, 6)
        val expected = ActivePiece(Tetromino.O, Rotation.SPAWN, origin).cells().toSet()

        Rotation.entries.forEach { rotation ->
            assertEquals(expected, ActivePiece(Tetromino.O, rotation, origin).cells().toSet())
        }
    }

    @Test
    fun `i piece rotates around its srs pivot`() {
        val origin = Cell(4, 6)

        assertEquals(
            setOf(Cell(3, 6), Cell(4, 6), Cell(5, 6), Cell(6, 6)),
            ActivePiece(Tetromino.I, Rotation.SPAWN, origin).cells().toSet(),
        )
        assertEquals(
            setOf(Cell(5, 5), Cell(5, 6), Cell(5, 7), Cell(5, 8)),
            ActivePiece(Tetromino.I, Rotation.RIGHT, origin).cells().toSet(),
        )
    }

    @Test
    fun `all four clockwise transitions advance to the next rotation`() {
        val transitions = listOf(
            Rotation.SPAWN to Rotation.RIGHT,
            Rotation.RIGHT to Rotation.REVERSE,
            Rotation.REVERSE to Rotation.LEFT,
            Rotation.LEFT to Rotation.SPAWN,
        )

        transitions.forEach { (from, to) ->
            val rotated = tryRotateClockwise(
                ActivePiece(Tetromino.T, from, Cell(4, 8)),
                Board.empty(),
            )
            assertEquals(to, assertNotNull(rotated).rotation)
        }
    }

    @Test
    fun `clockwise t rotation kicks away from left wall`() {
        val rotated = tryRotateClockwise(
            ActivePiece(Tetromino.T, Rotation.RIGHT, Cell(0, 6)),
            Board.empty(),
        )

        assertNotNull(rotated)
        assertEquals(Rotation.REVERSE, rotated.rotation)
        assertEquals(1, rotated.origin.x)
        assertTrue(canOccupy(rotated, Board.empty()))
    }

    @Test
    fun `clockwise t rotation uses floor kick`() {
        val rotated = tryRotateClockwise(
            ActivePiece(Tetromino.T, Rotation.SPAWN, Cell(4, Board.TOTAL_HEIGHT - 1)),
            Board.empty(),
        )

        assertNotNull(rotated)
        assertEquals(Rotation.RIGHT, rotated.rotation)
        assertTrue(rotated.origin.y < Board.TOTAL_HEIGHT - 1)
        assertTrue(canOccupy(rotated, Board.empty()))
    }

    @Test
    fun `occupied cells reject placement`() {
        val board = Board(
            List<Tetromino?>(Board.WIDTH * Board.TOTAL_HEIGHT) { index ->
                if (index == 6 * Board.WIDTH + 4) Tetromino.Z else null
            },
        )

        assertFalse(canOccupy(ActivePiece(Tetromino.T, Rotation.SPAWN, Cell(4, 6)), board))
    }

    @Test
    fun `rotation is rejected when every kick collides with the stack`() {
        val fullBoard = Board(List(Board.WIDTH * Board.TOTAL_HEIGHT) { Tetromino.J })

        assertEquals(
            null,
            tryRotateClockwise(
                ActivePiece(Tetromino.T, Rotation.SPAWN, Cell(4, 8)),
                fullBoard,
            ),
        )
    }
}
