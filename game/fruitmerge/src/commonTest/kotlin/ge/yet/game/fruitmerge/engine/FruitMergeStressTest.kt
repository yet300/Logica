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
import kotlin.test.assertTrue

class FruitMergeStressTest {
    @Test
    fun `one simulated minute remains finite and bounded`() {
        val engine = FruitMergeEngine()
        var state = FruitMergeState(
            bodies = List(MAX_BODIES) { index ->
                FruitBody(
                    id = index.toLong() + 1,
                    level = FruitLevel.BLUEBERRY,
                    position = Vec2(
                        x = 0.05f + (index % 10) * 0.095f,
                        y = 0.20f + (index / 10) * 0.095f,
                    ),
                )
            },
            nextBodyId = MAX_BODIES.toLong() + 1,
            graceSeconds = 60f,
        )

        repeat(60 * 60) { state = engine.step(state, 1f / 60f) }

        assertTrue(state.bodies.size <= MAX_BODIES)
        assertTrue(state.bodies.all { it.position.isFinite() && it.velocity.isFinite() })
        assertTrue(engine.diagnostics.maxCandidatePairs <= MAX_CANDIDATE_PAIRS)
    }
}
