package ge.yet.game.fruitmerge.engine

import ge.yet.game.fruitmerge.domain.engine.BodyPair
import ge.yet.game.fruitmerge.domain.engine.FruitMergeEngine
import ge.yet.game.fruitmerge.domain.engine.FruitMergeRules
import ge.yet.game.fruitmerge.domain.engine.FruitPhysics
import ge.yet.game.fruitmerge.domain.engine.MAX_BODIES
import ge.yet.game.fruitmerge.domain.engine.MAX_CANDIDATE_PAIRS
import ge.yet.game.fruitmerge.domain.engine.PhysicsResult
import ge.yet.game.fruitmerge.domain.engine.RandomState
import ge.yet.game.fruitmerge.domain.engine.SpatialGrid
import ge.yet.game.fruitmerge.domain.model.ActionRejection
import ge.yet.game.fruitmerge.domain.model.ActionResult
import ge.yet.game.fruitmerge.domain.model.EngineDiagnostics
import ge.yet.game.fruitmerge.domain.model.FruitBody
import ge.yet.game.fruitmerge.domain.model.FruitLevel
import ge.yet.game.fruitmerge.domain.model.FruitMergeState
import ge.yet.game.fruitmerge.domain.model.RunPhase
import ge.yet.game.fruitmerge.domain.model.TargetingMode
import ge.yet.game.fruitmerge.domain.model.Vec2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SpatialGridTest {
    @Test
    fun `maximum board stays below all pairs candidate count`() {
        val bodies = List(MAX_BODIES) { index ->
            FruitBody(
                id = index.toLong() + 1,
                level = FruitLevel.BLUEBERRY,
                position = Vec2(
                    x = 0.05f + (index % 10) * 0.095f,
                    y = 0.20f + (index / 10) * 0.095f,
                ),
            )
        }

        val pairs = SpatialGrid().candidatePairs(bodies)

        assertTrue(pairs.size < bodies.size * 12)
        assertEquals(pairs.distinct(), pairs)
    }

    @Test
    fun `candidate pair objects are pooled across fixed steps`() {
        val bodies = listOf(
            FruitBody(1, FruitLevel.WATERMELON, Vec2(0.4f, 0.7f)),
            FruitBody(2, FruitLevel.WATERMELON, Vec2(0.6f, 0.7f)),
        )
        val grid = SpatialGrid()
        val first = grid.candidatePairs(bodies).single()
        val second = grid.candidatePairs(bodies).single()

        assertSame(first, second)
    }
}
