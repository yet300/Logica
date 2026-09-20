package ge.yet.game.fallingblocks.engine

import ge.yet.game.fallingblocks.boardWith
import ge.yet.game.fallingblocks.domain.engine.DefaultFallingBlocksEngine
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.GameAction
import ge.yet.game.fallingblocks.domain.model.GameFact
import ge.yet.game.fallingblocks.domain.model.GamePhase
import ge.yet.game.fallingblocks.domain.model.Tetromino
import ge.yet.game.fallingblocks.gameFixture
import ge.yet.game.fallingblocks.hiddenRowLockFixture
import ge.yet.game.fallingblocks.spawnBlockedFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerminalAndReviveTest {
    private val engine = DefaultFallingBlocksEngine

    @Test
    fun `spawn collision tops out exactly once`() {
        val first = engine.reduce(spawnBlockedFixture(), GameAction.HardDrop)
        val second = engine.reduce(first.state, GameAction.AdvanceTime(1_000))

        assertEquals(GamePhase.TERMINAL, first.state.phase)
        assertEquals(1, first.facts.count { it is GameFact.ToppedOut })
        assertEquals(first.state, second.state)
        assertTrue(second.facts.isEmpty())
    }

    @Test
    fun `locked hidden cell tops out`() {
        val result = engine.reduce(hiddenRowLockFixture(), GameAction.HardDrop)

        assertEquals(GamePhase.TERMINAL, result.state.phase)
        assertTrue(result.facts.any { it is GameFact.ToppedOut })
    }

    @Test
    fun `revive clears bottom rows shifts the rest and preserves progression`() {
        val terminal = gameFixture(
            board = boardWith(
                Cell(1, 5) to Tetromino.J,
                Cell(2, Board.TOTAL_HEIGHT - 1) to Tetromino.L,
            ),
            score = 9_000,
            level = 7,
            lines = 63,
            combo = 4,
            backToBack = true,
            phase = GamePhase.TERMINAL,
        )

        val revived = engine.reduce(terminal, GameAction.Revive)

        assertEquals(terminal.score, revived.state.score)
        assertEquals(terminal.level, revived.state.level)
        assertEquals(terminal.lines, revived.state.lines)
        assertEquals(terminal.preview, revived.state.preview)
        assertEquals(terminal.bag, revived.state.bag)
        assertEquals(Tetromino.J, revived.state.board[Cell(1, 9)])
        assertEquals(null, revived.state.board[Cell(2, Board.TOTAL_HEIGHT - 1)])
        assertEquals(1, revived.state.revivesUsed)
        assertFalse(revived.state.backToBack)
        assertEquals(-1, revived.state.combo)
        assertEquals(GamePhase.PLAYING, revived.state.phase)
        assertEquals(listOf(GameFact.Revived), revived.facts)
    }

    @Test
    fun `revive can only be used once`() {
        val terminal = gameFixture(phase = GamePhase.TERMINAL, revivesUsed = 1)

        val rejected = engine.reduce(terminal, GameAction.Revive)

        assertEquals(terminal, rejected.state)
        assertEquals(listOf(GameFact.Blocked), rejected.facts)
    }
}
