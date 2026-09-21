package ge.yet.game.fallingblocks.engine

import ge.yet.game.fallingblocks.boardWith
import ge.yet.game.fallingblocks.domain.engine.DefaultFallingBlocksEngine
import ge.yet.game.fallingblocks.domain.model.ActivePiece
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.GameAction
import ge.yet.game.fallingblocks.domain.model.Rotation
import ge.yet.game.fallingblocks.domain.model.Tetromino
import ge.yet.game.fallingblocks.gameFixture
import kotlin.test.Test
import kotlin.test.assertEquals

class TSpinTest {
    private val engine = DefaultFallingBlocksEngine

    @Test
    fun `three occupied pivot corners after rotation score a t spin`() {
        val state = gameFixture(
            board = boardWith(
                Cell(3, 19) to Tetromino.J,
                Cell(5, 19) to Tetromino.J,
                Cell(3, 21) to Tetromino.J,
            ),
            active = ActivePiece(Tetromino.T, Rotation.RIGHT, Cell(4, 20)),
            lastActionWasRotation = true,
        )

        val result = engine.reduce(state, GameAction.HardDrop)

        assertEquals(400, result.state.score)
    }

    @Test
    fun `same placement without a last rotation is not a t spin`() {
        val state = gameFixture(
            board = boardWith(
                Cell(3, 19) to Tetromino.J,
                Cell(5, 19) to Tetromino.J,
                Cell(3, 21) to Tetromino.J,
            ),
            active = ActivePiece(Tetromino.T, Rotation.RIGHT, Cell(4, 20)),
            lastActionWasRotation = false,
        )

        assertEquals(0, engine.reduce(state, GameAction.HardDrop).state.score)
    }
}
