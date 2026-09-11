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
import kotlin.test.assertTrue

class RandomStateTest {
    @Test
    fun `same seed produces the same bounded sequence`() {
        fun sequence(seed: Long): List<Int> {
            var state = RandomState(seed)
            return List(32) {
                val next = state.nextInt()
                state = next.state
                next.value
            }
        }

        val first = sequence(7)
        assertEquals(first, sequence(7))
        assertTrue(first.all { it in 0 until Int.MAX_VALUE })
    }
}
