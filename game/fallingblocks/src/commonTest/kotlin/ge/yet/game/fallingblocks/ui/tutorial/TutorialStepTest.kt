package ge.yet.game.fallingblocks.ui.tutorial

import ge.yet.game.fallingblocks.domain.model.GameFact
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TutorialStepTest {
    @Test
    fun `only matching legal fact advances each tutorial step`() {
        val initial = TutorialProgress.initial()
        assertEquals(TutorialStep.ROTATE, initial.step)
        assertEquals(TutorialStep.ROTATE, initial.accept(listOf(GameFact.Moved(1, 0))).step)
        assertEquals(TutorialStep.ROTATE, initial.accept(listOf(GameFact.Blocked)).step)

        val moved = initial
            .accept(listOf(GameFact.Rotated))
            .accept(listOf(GameFact.Moved(1, 0)))
        assertEquals(TutorialStep.SOFT_DROP, moved.step)

        val dropped = moved.accept(listOf(GameFact.Moved(0, 1)))
        assertEquals(TutorialStep.HARD_DROP, dropped.step)
        assertFalse(dropped.complete)
        assertTrue(dropped.accept(listOf(GameFact.HardDropped(8))).complete)
    }

    @Test
    fun `facts from the wrong axis never skip steps`() {
        val progress = TutorialProgress(TutorialStep.MOVE)

        assertEquals(TutorialStep.MOVE, progress.accept(listOf(GameFact.Moved(0, 4))).step)
        assertEquals(TutorialStep.MOVE, progress.accept(listOf(GameFact.HardDropped(12))).step)
    }
}
