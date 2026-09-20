package ge.yet.game.fallingblocks

import kotlin.test.Test
import kotlin.test.assertEquals

class FallingblocksGameEngineTest {
    @Test
    fun `reset returns the initial state`() {
        val state = FallingblocksGameState(score = 42, isGameOver = true)

        assertEquals(
            FallingblocksGameState(),
            DefaultFallingblocksGameEngine.reduce(state, FallingblocksGameAction.Reset),
        )
    }

    @Test
    fun `tick is an explicit placeholder for game rules`() {
        val state = FallingblocksGameState(score = 7)

        assertEquals(
            state,
            DefaultFallingblocksGameEngine.reduce(state, FallingblocksGameAction.Tick),
        )
    }
}
