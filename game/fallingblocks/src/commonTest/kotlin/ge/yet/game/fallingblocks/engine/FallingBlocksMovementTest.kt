package ge.yet.game.fallingblocks.engine

import ge.yet.game.fallingblocks.gameFixture
import ge.yet.game.fallingblocks.domain.engine.DefaultFallingBlocksEngine
import ge.yet.game.fallingblocks.domain.model.ActivePiece
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.GameAction
import ge.yet.game.fallingblocks.domain.model.GameFact
import ge.yet.game.fallingblocks.domain.model.Rotation
import ge.yet.game.fallingblocks.domain.model.Tetromino
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FallingBlocksMovementTest {
    private val engine = DefaultFallingBlocksEngine

    @Test
    fun `horizontal move applies legal single cell steps until the wall`() {
        val result = engine.reduce(gameFixture(), GameAction.MoveHorizontal(20))

        assertEquals(8, result.state.active.origin.x)
        assertEquals(listOf(GameFact.Moved(horizontalCells = 5, downwardCells = 0)), result.facts)
    }

    @Test
    fun `blocked horizontal move leaves state unchanged`() {
        val state = gameFixture(
            active = ActivePiece(Tetromino.T, Rotation.SPAWN, Cell(1, 8)),
        )

        val result = engine.reduce(state, GameAction.MoveHorizontal(-1))
        val blocked = engine.reduce(result.state, GameAction.MoveHorizontal(-1))

        assertEquals(result.state, blocked.state)
        assertEquals(listOf(GameFact.Blocked), blocked.facts)
    }

    @Test
    fun `clockwise rotation emits a typed fact`() {
        val result = engine.reduce(gameFixture(), GameAction.RotateClockwise)

        assertEquals(Rotation.RIGHT, result.state.active.rotation)
        assertTrue(result.facts.single() is GameFact.Rotated)
    }

    @Test
    fun `initial state has one active piece and five previews`() {
        val state = engine.initial(seed = 42, runId = 9)

        assertEquals(5, state.preview.size)
        assertEquals(9, state.runId)
        assertEquals(6, (state.preview + state.active.type).toSet().size)
    }
}
