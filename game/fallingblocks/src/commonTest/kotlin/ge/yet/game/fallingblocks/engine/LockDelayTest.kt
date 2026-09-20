package ge.yet.game.fallingblocks.engine

import ge.yet.game.fallingblocks.domain.engine.DefaultFallingBlocksEngine
import ge.yet.game.fallingblocks.domain.engine.gravityMillis
import ge.yet.game.fallingblocks.domain.model.GameAction
import ge.yet.game.fallingblocks.domain.model.GameFact
import ge.yet.game.fallingblocks.restingFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LockDelayTest {
    private val engine = DefaultFallingBlocksEngine

    @Test
    fun `first blocked gravity step starts delay without locking`() {
        val result = engine.reduce(restingFixture(), GameAction.AdvanceTime(499))

        assertFalse(result.facts.any { it is GameFact.Locked })
        assertEquals(1, result.state.lockRemainingMillis)
    }

    @Test
    fun `lock occurs exactly at five hundred milliseconds`() {
        val result = engine.reduce(restingFixture(), GameAction.AdvanceTime(500))

        assertEquals(1, result.facts.count { it is GameFact.Locked })
    }

    @Test
    fun `valid grounded movement resets lock delay no more than fifteen times`() {
        val reset = engine.reduce(
            restingFixture(lockRemainingMillis = 1, lockResetCount = 14),
            GameAction.MoveHorizontal(1),
        ).state
        val capped = engine.reduce(
            restingFixture(lockRemainingMillis = 1, lockResetCount = 15),
            GameAction.MoveHorizontal(1),
        ).state

        assertEquals(500, reset.lockRemainingMillis)
        assertEquals(15, reset.lockResetCount)
        assertEquals(1, capped.lockRemainingMillis)
        assertEquals(15, capped.lockResetCount)
    }

    @Test
    fun `gravity table reaches but never crosses eighty milliseconds`() {
        assertEquals(800, gravityMillis(level = 1))
        assertEquals(80, gravityMillis(level = 18))
        assertEquals(80, gravityMillis(level = 999))
    }
}
