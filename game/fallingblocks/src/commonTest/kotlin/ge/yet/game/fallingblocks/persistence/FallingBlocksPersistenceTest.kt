package ge.yet.game.fallingblocks.persistence

import ge.yet.game.fallingblocks.data.BEST_SCORE_KEY
import ge.yet.game.fallingblocks.data.FallingBlocksPersistence
import ge.yet.game.fallingblocks.data.FallingBlocksSnapshotSpec
import ge.yet.game.fallingblocks.data.GameSnapshotV1
import ge.yet.game.fallingblocks.data.SNAPSHOT_KEY
import ge.yet.game.fallingblocks.data.TUTORIAL_SEEN_KEY
import ge.yet.game.fallingblocks.domain.repository.CommitResult
import ge.yet.game.fallingblocks.gameFixture
import ge.yet.game.miniapp.testkit.MutableMiniAppStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FallingBlocksPersistenceTest {
    @Test
    fun `write and load preserve exact state best score and tutorial`() = runTest {
        val storage = MutableMiniAppStorage()
        val persistence = FallingBlocksPersistence(storage)
        val state = gameFixture(score = 700, gravityRemainingMillis = 123, lockRemainingMillis = 77)

        assertIs<CommitResult.Stored>(persistence.write(state))
        persistence.markTutorialSeen()
        val restored = persistence.load()

        assertEquals(state, restored.state)
        assertEquals(700, restored.bestScore)
        assertTrue(restored.tutorialSeen)
        assertEquals(GameSnapshotV1.from(state), storage.readSnapshot(SNAPSHOT_KEY, FallingBlocksSnapshotSpec))
    }

    @Test
    fun `corrupt snapshot is ignored while independent best and tutorial survive`() = runTest {
        val corrupt = GameSnapshotV1.from(gameFixture()).copy(preview = emptyList())
        val storage = MutableMiniAppStorage(
            mapOf(
                SNAPSHOT_KEY to corrupt,
                BEST_SCORE_KEY to 5_000L,
                TUTORIAL_SEEN_KEY to true,
            ),
        )

        val restored = FallingBlocksPersistence(storage).load()

        assertNull(restored.state)
        assertEquals(5_000, restored.bestScore)
        assertTrue(restored.tutorialSeen)
    }
}
