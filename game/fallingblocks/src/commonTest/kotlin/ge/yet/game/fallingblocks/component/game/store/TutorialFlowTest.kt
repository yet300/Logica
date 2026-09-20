package ge.yet.game.fallingblocks.component.game.store

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import ge.yet.game.fallingblocks.domain.model.FallingBlocksEngine
import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.domain.model.GameAction
import ge.yet.game.fallingblocks.domain.model.GameFact
import ge.yet.game.fallingblocks.domain.model.GameTransition
import ge.yet.game.fallingblocks.domain.repository.CommitResult
import ge.yet.game.fallingblocks.domain.repository.GameCommitWriter
import ge.yet.game.fallingblocks.domain.repository.GameSnapshotLoader
import ge.yet.game.fallingblocks.domain.repository.RestoredSession
import ge.yet.game.fallingblocks.domain.repository.TutorialSeenRepository
import ge.yet.game.fallingblocks.gameFixture
import ge.yet.game.fallingblocks.NoOpFallingBlocksAudioPlayer
import ge.yet.game.miniapp.testkit.MutableMiniAppVisibilitySource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TutorialFlowTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `completion persists seen before starting a clean real run`() = runTest {
        val operations = mutableListOf<String>()
        val engine = TutorialEngine()
        val store = FallingBlocksStoreFactory(
            storeFactory = DefaultStoreFactory(),
            engine = engine,
            loader = object : GameSnapshotLoader {
                override suspend fun load(): RestoredSession = RestoredSession(null, 0, false)
            },
            writer = object : GameCommitWriter {
                override suspend fun write(state: FallingBlocksState): CommitResult {
                    operations += "new_game"
                    return CommitResult.Stored
                }
            },
            tutorial = object : TutorialSeenRepository {
                override suspend fun isTutorialSeen(): Boolean = false
                override suspend fun markTutorialSeen() {
                    operations += "tutorial_seen"
                }
            },
            visibility = MutableMiniAppVisibilitySource(),
            seedSource = NewGameSeedSource { 77L },
            audio = NoOpFallingBlocksAudioPlayer,
        ).create()
        advanceUntilIdle()

        assertFalse(store.state.tutorialSeen)
        assertTrue(store.state.tutorialProgress != null)
        store.accept(FallingBlocksStore.Intent.Rotate)
        store.accept(FallingBlocksStore.Intent.Move(1))
        store.accept(FallingBlocksStore.Intent.SoftDrop(1))
        store.accept(FallingBlocksStore.Intent.HardDrop)
        advanceUntilIdle()

        assertEquals(listOf("tutorial_seen", "new_game"), operations)
        assertTrue(store.state.tutorialSeen)
        assertNull(store.state.tutorialProgress)
        assertEquals(0L, store.state.game?.score)
        assertTrue(store.state.game?.board?.cells?.all { it == null } == true)
        assertEquals(listOf(TUTORIAL_SEED, 77L), engine.initialSeeds)
        store.dispose()
    }
}

private class TutorialEngine : FallingBlocksEngine {
    val initialSeeds = mutableListOf<Long>()

    override fun initial(seed: Long, runId: Long): FallingBlocksState {
        initialSeeds += seed
        return gameFixture().copy(runId = runId)
    }

    override fun reduce(state: FallingBlocksState, action: GameAction): GameTransition {
        val fact = when (action) {
            GameAction.RotateClockwise -> GameFact.Rotated
            is GameAction.MoveHorizontal -> GameFact.Moved(action.cells, 0)
            is GameAction.SoftDrop -> GameFact.Moved(0, action.cells)
            GameAction.HardDrop -> GameFact.HardDropped(10)
            is GameAction.AdvanceTime -> GameFact.Moved(0, 1)
            GameAction.Revive -> GameFact.Revived
        }
        return GameTransition(state.copy(score = state.score + 10), listOf(fact))
    }
}
