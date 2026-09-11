package ge.yet.game.fruitmerge.session.store

import ge.yet.game.fruitmerge.domain.engine.FruitMergeEngine
import ge.yet.game.fruitmerge.domain.model.FruitBody
import ge.yet.game.fruitmerge.domain.model.FruitLevel
import ge.yet.game.fruitmerge.domain.model.FruitMergeState
import ge.yet.game.fruitmerge.domain.model.RunPhase
import ge.yet.game.fruitmerge.domain.model.Vec2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FruitMergeTransitionPlannerTest {
    @Test
    fun `joining pile emits landing in id order`() {
        val before = FruitMergeState(
            bodies = listOf(
                FruitBody(id = 2L, level = FruitLevel.LIME, position = Vec2(0.6f, 0.7f)),
                FruitBody(id = 1L, level = FruitLevel.APPLE, position = Vec2(0.4f, 0.7f)),
            ),
        )
        val after = before.copy(
            bodies = before.bodies.map { body -> body.copy(hasJoinedPile = true) },
        )

        assertEquals(
            listOf(
                FruitMergeStore.Label.FruitLanded(FruitLevel.APPLE, Vec2(0.4f, 0.7f)),
                FruitMergeStore.Label.FruitLanded(FruitLevel.LIME, Vec2(0.6f, 0.7f)),
            ),
            FruitMergeTransitionPlanner.stepLabels(before, after),
        )
    }

    @Test
    fun `settled pile emits no landing`() {
        val state = FruitMergeState(
            bodies = listOf(
                FruitBody(
                    id = 1L,
                    level = FruitLevel.APPLE,
                    position = Vec2(0.5f, 0.8f),
                    hasJoinedPile = true,
                ),
            ),
        )

        assertTrue(FruitMergeTransitionPlanner.stepLabels(state, state).isEmpty())
    }

    @Test
    fun `score growth emits merge for created bodies`() {
        val before = FruitMergeState(
            bodies = listOf(FruitBody(id = 1L, level = FruitLevel.LIME, position = Vec2(0.5f, 0.8f))),
            score = 10L,
        )
        val created = FruitBody(id = 3L, level = FruitLevel.MANDARIN, position = Vec2(0.5f, 0.7f))
        val after = before.copy(bodies = listOf(created), score = 65L)

        assertEquals(
            listOf(FruitMergeStore.Label.MergeResolved(FruitLevel.MANDARIN, Vec2(0.5f, 0.7f))),
            FruitMergeTransitionPlanner.stepLabels(before, after),
        )
    }

    @Test
    fun `removed watermelon pair emits a midpoint merge`() {
        val first = FruitBody(id = 1L, level = FruitLevel.WATERMELON, position = Vec2(0.4f, 0.8f))
        val second = FruitBody(id = 2L, level = FruitLevel.WATERMELON, position = Vec2(0.6f, 0.8f))
        val before = FruitMergeState(bodies = listOf(first, second), score = 100L)
        val after = before.copy(bodies = emptyList(), score = 4_500L)

        assertEquals(
            listOf(
                FruitMergeStore.Label.MergeResolved(
                    level = FruitLevel.WATERMELON,
                    position = Vec2(0.5f, 0.8f),
                ),
            ),
            FruitMergeTransitionPlanner.stepLabels(before, after),
        )
    }

    @Test
    fun `shake impulse boundary emits pulse index`() {
        val before = FruitMergeState(shakeStepsRemaining = FruitMergeEngine.SHAKE_DURATION_STEPS)

        assertEquals(
            listOf(FruitMergeStore.Label.ShakePulse(0)),
            FruitMergeTransitionPlanner.stepLabels(before, before),
        )
    }

    @Test
    fun `off boundary shake emits no pulse`() {
        val before = FruitMergeState(shakeStepsRemaining = FruitMergeEngine.SHAKE_DURATION_STEPS - 1)

        assertTrue(FruitMergeTransitionPlanner.stepLabels(before, before).isEmpty())
    }

    @Test
    fun `danger onset emits entered once`() {
        val before = FruitMergeState(dangerSeconds = 0f)
        val after = before.copy(dangerSeconds = 0.5f)

        assertEquals(
            listOf(FruitMergeStore.Label.DangerEntered),
            FruitMergeTransitionPlanner.stepLabels(before, after),
        )
        assertTrue(FruitMergeTransitionPlanner.stepLabels(after, after).isEmpty())
    }

    @Test
    fun `result reached only on playing to result transition`() {
        val playing = FruitMergeState(phase = RunPhase.PLAYING)
        val result = FruitMergeState(phase = RunPhase.RESULT)

        assertTrue(FruitMergeTransitionPlanner.resultReached(playing, result))
        assertFalse(FruitMergeTransitionPlanner.resultReached(playing, playing))
        assertFalse(FruitMergeTransitionPlanner.resultReached(result, result))
    }
}
