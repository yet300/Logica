package ge.yet.game.uikit.components.score

import kotlin.test.Test
import kotlin.test.assertEquals

class ScoreCardStateTest {
    @Test
    fun `zero best presents only the current score`() {
        assertEquals(
            ScoreCardState.ScoreOnly,
            scoreCardState(bestScore = 0L, bestImprovedInRun = false),
        )
    }

    @Test
    fun `persisted best presents score and best`() {
        assertEquals(
            ScoreCardState.ScoreAndBest,
            scoreCardState(bestScore = 1_200L, bestImprovedInRun = false),
        )
    }

    @Test
    fun `improved run presents best only`() {
        assertEquals(
            ScoreCardState.BestOnly,
            scoreCardState(bestScore = 1_200L, bestImprovedInRun = true),
        )
    }
}
