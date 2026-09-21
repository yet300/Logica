package ge.yet.game.fallingblocks.engine

import ge.yet.game.fallingblocks.boardWith
import ge.yet.game.fallingblocks.domain.engine.DefaultFallingBlocksEngine
import ge.yet.game.fallingblocks.domain.model.ActivePiece
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.GameAction
import ge.yet.game.fallingblocks.domain.model.GameFact
import ge.yet.game.fallingblocks.domain.model.Rotation
import ge.yet.game.fallingblocks.domain.model.Tetromino
import ge.yet.game.fallingblocks.gameFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LineClearTest {
    private val engine = DefaultFallingBlocksEngine

    @Test
    fun `hard drop clears a row and awards distance plus perfect clear`() {
        val occupied = (0 until Board.WIDTH)
            .filterNot { it in 3..6 }
            .map { x -> Cell(x, Board.TOTAL_HEIGHT - 1) to Tetromino.J }
            .toTypedArray()
        val state = gameFixture(
            board = boardWith(*occupied),
            active = ActivePiece(Tetromino.I, Rotation.SPAWN, Cell(4, 5)),
        )

        val result = engine.reduce(state, GameAction.HardDrop)

        assertTrue(result.state.board.cells.all { it == null })
        assertEquals(1, result.state.lines)
        assertEquals(1, result.state.level)
        assertEquals(2L * 16 + 100L + 2_000L, result.state.score)
        assertTrue(
            result.facts.contains(
                GameFact.LinesCleared(
                    rows = listOf(Board.TOTAL_HEIGHT - 1),
                    perfect = true,
                ),
            ),
        )
    }

    @Test
    fun `ten total lines advance the level`() {
        val occupied = (0 until Board.WIDTH)
            .filterNot { it in 3..6 }
            .map { x -> Cell(x, Board.TOTAL_HEIGHT - 1) to Tetromino.L }
            .toTypedArray()
        val result = engine.reduce(
            gameFixture(
                board = boardWith(*occupied),
                active = ActivePiece(Tetromino.I, Rotation.SPAWN, Cell(4, Board.TOTAL_HEIGHT - 1)),
                lines = 9,
            ),
            GameAction.HardDrop,
        )

        assertEquals(2, result.state.level)
        assertTrue(result.facts.contains(GameFact.LevelChanged(2)))
    }

    @Test
    fun `soft drop awards one point per descended cell without locking`() {
        val result = engine.reduce(gameFixture(), GameAction.SoftDrop(3))

        assertEquals(3, result.state.active.origin.y - 2)
        assertEquals(3, result.state.score)
        assertTrue(result.facts.contains(GameFact.Moved(horizontalCells = 0, downwardCells = 3)))
    }

    @Test
    fun `first clear starts combo at zero and a normal clear breaks back to back`() {
        val occupied = (0 until Board.WIDTH)
            .filterNot { it in 3..6 }
            .map { x -> Cell(x, Board.TOTAL_HEIGHT - 1) to Tetromino.S }
            .toTypedArray()

        val result = engine.reduce(
            gameFixture(
                board = boardWith(*occupied),
                active = ActivePiece(Tetromino.I, Rotation.SPAWN, Cell(4, Board.TOTAL_HEIGHT - 1)),
                combo = -1,
                backToBack = true,
            ),
            GameAction.HardDrop,
        )

        assertEquals(0, result.state.combo)
        assertEquals(false, result.state.backToBack)
    }

    @Test
    fun `four line clear starts and continues back to back`() {
        val occupied = (Board.TOTAL_HEIGHT - 4 until Board.TOTAL_HEIGHT)
            .flatMap { y ->
                (0 until Board.WIDTH)
                    .filterNot { x -> x == 5 }
                    .map { x -> Cell(x, y) to Tetromino.Z }
            }
            .toTypedArray()
        val base = gameFixture(
            board = boardWith(*occupied),
            active = ActivePiece(Tetromino.I, Rotation.RIGHT, Cell(4, Board.TOTAL_HEIGHT - 3)),
        )

        val startedTransition = engine.reduce(base, GameAction.HardDrop)
        val continuedTransition = engine.reduce(
            base.copy(backToBack = true, combo = 0),
            GameAction.HardDrop,
        )
        val started = startedTransition.state
        val continued = continuedTransition.state

        assertTrue(started.backToBack)
        assertEquals(2_800, started.score)
        assertTrue(continued.backToBack)
        assertEquals(3_250, continued.score)
        assertEquals(
            (Board.TOTAL_HEIGHT - 4 until Board.TOTAL_HEIGHT).toList(),
            startedTransition.facts.filterIsInstance<GameFact.LinesCleared>().single().rows,
        )
    }
}
