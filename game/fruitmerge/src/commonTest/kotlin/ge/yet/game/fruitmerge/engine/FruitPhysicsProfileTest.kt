package ge.yet.game.fruitmerge.engine

import ge.yet.game.fruitmerge.domain.engine.BodyPair
import ge.yet.game.fruitmerge.domain.engine.FruitMergeEngine
import ge.yet.game.fruitmerge.domain.engine.FruitMergeRules
import ge.yet.game.fruitmerge.domain.engine.FruitPhysics
import ge.yet.game.fruitmerge.domain.engine.fruitPhysicsProfile
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

class FruitPhysicsProfileTest {
    @Test
    fun `every tier has one finite bounded profile`() {
        FruitLevel.entries.forEach { level ->
            val profile = fruitPhysicsProfile(level)

            assertTrue(profile.massMultiplier.isFinite())
            assertTrue(profile.contactRestitution.isFinite())
            assertTrue(profile.floorRetention.isFinite())
            assertTrue(profile.wallGripSeconds.isFinite())
            assertTrue(profile.spinTransfer.isFinite())
            assertTrue(profile.balanceTorque.isFinite())
            assertTrue(profile.shockImpulse.isFinite())
            assertTrue(profile.massMultiplier in 0.5f..1.5f)
            assertTrue(profile.contactRestitution in 0f..0.5f)
            assertTrue(profile.floorRetention in 0f..1f)
            assertTrue(profile.wallGripSeconds in 0f..0.5f)
            assertTrue(profile.spinTransfer in 0f..0.8f)
            assertTrue(profile.balanceTorque in -0.4f..0.4f)
            assertTrue(profile.shockImpulse in 0f..0.7f)
        }
    }

    @Test
    fun `signature traits belong to exact fruit tiers`() {
        assertTrue(fruitPhysicsProfile(FruitLevel.BLUEBERRY).contactRestitution > 0.2f)
        assertTrue(fruitPhysicsProfile(FruitLevel.RASPBERRY).floorRetention < 0.7f)
        assertTrue(fruitPhysicsProfile(FruitLevel.STRAWBERRY).wallGripSeconds > 0f)
        assertTrue(fruitPhysicsProfile(FruitLevel.LIME).spinTransfer > 0.5f)
        assertTrue(fruitPhysicsProfile(FruitLevel.MANDARIN).floorRetention > 0.9f)
        assertTrue(fruitPhysicsProfile(FruitLevel.APPLE).massMultiplier > 1f)
        assertTrue(fruitPhysicsProfile(FruitLevel.PEAR).balanceTorque != 0f)
        assertTrue(fruitPhysicsProfile(FruitLevel.PEACH).contactRestitution < 0.05f)
        assertTrue(fruitPhysicsProfile(FruitLevel.PINEAPPLE).floorRetention < 0.6f)
        assertTrue(fruitPhysicsProfile(FruitLevel.WATERMELON).shockImpulse > 0f)
    }
}
