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

class FruitCatalogTest {
    @Test
    fun `catalog exposes the market identities in stable merge order`() {
        assertEquals(
            listOf(
                FruitLevel.BLUEBERRY,
                FruitLevel.RASPBERRY,
                FruitLevel.STRAWBERRY,
                FruitLevel.LIME,
                FruitLevel.MANDARIN,
                FruitLevel.APPLE,
                FruitLevel.PEAR,
                FruitLevel.PEACH,
                FruitLevel.PINEAPPLE,
                FruitLevel.WATERMELON,
            ),
            FruitLevel.entries,
        )
    }

    @Test
    fun `legacy fruit names restore to their market identities`() {
        assertEquals(FruitLevel.RASPBERRY, FruitLevel.fromPersistedName("CHERRY"))
        assertEquals(FruitLevel.LIME, FruitLevel.fromPersistedName("PLUM"))
        assertEquals(FruitLevel.WATERMELON, FruitLevel.fromPersistedName("MELON"))
    }

    @Test
    fun `catalog has ten increasing levels and only first five spawn`() {
        assertEquals(10, FruitLevel.entries.size)
        assertTrue(
            FruitLevel.entries.zipWithNext().all { (first, second) ->
                second.radius > first.radius &&
                    second.mass > first.mass &&
                    second.mergeScore > first.mergeScore
            },
        )
        assertEquals(
            setOf(
                FruitLevel.BLUEBERRY,
                FruitLevel.RASPBERRY,
                FruitLevel.STRAWBERRY,
                FruitLevel.LIME,
                FruitLevel.MANDARIN,
            ),
            FruitLevel.spawnable.toSet(),
        )
    }
}
