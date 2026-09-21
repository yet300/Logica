package ge.yet.game.fallingblocks.component.game

import ge.yet.game.fallingblocks.boardWith
import ge.yet.game.fallingblocks.domain.engine.DefaultFallingBlocksEngine
import ge.yet.game.fallingblocks.domain.engine.cells
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
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class FallingBlocksTransitionPlannerTest {
    private val planner = FallingBlocksTransitionPlanner()

    @Test
    fun `hard drop captures original and landing cells`() {
        val before = gameFixture(
            active = ActivePiece(Tetromino.T, Rotation.SPAWN, Cell(4, 3)),
        )
        val transition = DefaultFallingBlocksEngine.reduce(before, GameAction.HardDrop)

        val event = assertIs<FallingBlocksVisualEvent.HardDrop>(
            planner.plan(before, GameAction.HardDrop, transition.state, transition.facts, nextId = 7),
        )

        assertEquals(7L, event.id)
        assertEquals(before.active.cells().toSet(), event.from.toSet())
        assertEquals(Board.TOTAL_HEIGHT - 1, event.to.maxOf(Cell::y))
    }

    @Test
    fun `line clear wins and captures complete pre collapse row`() {
        val row = Board.TOTAL_HEIGHT - 1
        val occupied = (0 until Board.WIDTH)
            .filterNot { it in 3..6 }
            .map { x -> Cell(x, row) to Tetromino.J }
            .toTypedArray()
        val before = gameFixture(
            board = boardWith(*occupied),
            active = ActivePiece(Tetromino.I, Rotation.SPAWN, Cell(4, 5)),
        )
        val transition = DefaultFallingBlocksEngine.reduce(before, GameAction.HardDrop)

        val event = assertIs<FallingBlocksVisualEvent.LineClear>(
            planner.plan(before, GameAction.HardDrop, transition.state, transition.facts, nextId = 11),
        )

        assertEquals(listOf(row), event.rows)
        assertEquals(Board.WIDTH, event.cells.size)
        assertEquals((0 until Board.WIDTH).toSet(), event.cells.map { it.cell.x }.toSet())
        assertEquals(setOf(row), event.cells.map { it.cell.y }.toSet())
    }

    @Test
    fun `normal movement produces no visual event`() {
        val before = gameFixture()
        val transition = DefaultFallingBlocksEngine.reduce(before, GameAction.MoveHorizontal(1))

        assertNull(
            planner.plan(before, GameAction.MoveHorizontal(1), transition.state, transition.facts, 1),
        )
    }

    @Test
    fun `event id overflow is rejected before wraparound`() {
        val before = gameFixture()
        val transition = DefaultFallingBlocksEngine.reduce(before, GameAction.HardDrop)

        assertFailsWith<IllegalArgumentException> {
            planner.plan(
                before,
                GameAction.HardDrop,
                transition.state,
                transition.facts,
                Long.MAX_VALUE,
            )
        }
    }
}
