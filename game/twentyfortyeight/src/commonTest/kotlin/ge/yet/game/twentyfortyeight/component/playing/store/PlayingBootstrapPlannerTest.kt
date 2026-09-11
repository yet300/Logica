package ge.yet.game.twentyfortyeight.component.playing.store

import ge.yet.game.twentyfortyeight.domain.engine.RngState
import ge.yet.game.twentyfortyeight.domain.model.GamePhase
import ge.yet.game.twentyfortyeight.domain.model.GameStatistics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayingBootstrapPlannerTest {
    @Test
    fun `unacknowledged victory restores victory overlay`() {
        val game = playableGame().copy(
            phase = GamePhase.Playing,
            facts = playableGame().facts.copy(victoryReached = true, victoryAcknowledged = false),
        )

        assertEquals(
            OverlayState.Victory,
            PlayingBootstrapPlanner.resolveRestoredOverlay(game),
        )
    }

    @Test
    fun `acknowledged victory restores no overlay`() {
        val game = playableGame().copy(
            phase = GamePhase.Playing,
            facts = playableGame().facts.copy(victoryReached = true, victoryAcknowledged = true),
        )

        assertNull(PlayingBootstrapPlanner.resolveRestoredOverlay(game))
    }

    @Test
    fun `non-playing game restores no overlay`() {
        val game = playableGame().copy(phase = GamePhase.GameOver)

        assertNull(PlayingBootstrapPlanner.resolveRestoredOverlay(game))
    }

    @Test
    fun `fresh rules start first run with best score carried over`() {
        val fresh = PlayingBootstrapPlanner.freshRules(
            restoredData(game = null, bestScore = 512L, statistics = GameStatistics()),
            seed = RngState.fromBits(7uL),
        )

        assertFalse(fresh.counterOverflow)
        assertEquals(1L, fresh.rules.game.runOrdinal)
        assertEquals(512L, fresh.rules.game.bestScore)
        assertEquals(1L, fresh.rules.statistics.gamesStarted)
    }

    @Test
    fun `fresh rules increment run counters and track highest tile`() {
        val fresh = PlayingBootstrapPlanner.freshRules(
            restoredData(
                game = null,
                statistics = GameStatistics(gamesStarted = 4L, highestTileEver = 128L),
            ),
            seed = RngState.fromBits(7uL),
        )

        assertFalse(fresh.counterOverflow)
        assertEquals(5L, fresh.rules.game.runOrdinal)
        assertEquals(5L, fresh.rules.statistics.gamesStarted)
        assertTrue(fresh.rules.statistics.highestTileEver >= 128L)
    }

    @Test
    fun `fresh rules flag counter overflow instead of wrapping`() {
        val fresh = PlayingBootstrapPlanner.freshRules(
            restoredData(game = null, statistics = GameStatistics(gamesStarted = Long.MAX_VALUE)),
            seed = RngState.fromBits(7uL),
        )

        assertTrue(fresh.counterOverflow)
        assertEquals(Long.MAX_VALUE, fresh.rules.statistics.gamesStarted)
        assertEquals(Long.MAX_VALUE, fresh.rules.game.runOrdinal)
    }
}
