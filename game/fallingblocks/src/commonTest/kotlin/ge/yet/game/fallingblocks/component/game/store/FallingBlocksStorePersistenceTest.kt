package ge.yet.game.fallingblocks.component.game.store

import com.arkivanov.mvikotlin.core.rx.observer
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import ge.yet.game.fallingblocks.domain.engine.DefaultFallingBlocksEngine
import ge.yet.game.fallingblocks.domain.model.FallingBlocksEngine
import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.domain.model.GameAction
import ge.yet.game.fallingblocks.domain.model.GameFact
import ge.yet.game.fallingblocks.domain.model.GamePhase
import ge.yet.game.fallingblocks.domain.model.GameTransition
import ge.yet.game.fallingblocks.domain.repository.CommitResult
import ge.yet.game.fallingblocks.domain.repository.GameCommitWriter
import ge.yet.game.fallingblocks.domain.repository.GameSnapshotLoader
import ge.yet.game.fallingblocks.domain.repository.RestoredSession
import ge.yet.game.fallingblocks.domain.repository.TutorialSeenRepository
import ge.yet.game.fallingblocks.gameFixture
import ge.yet.game.fallingblocks.NoOpFallingBlocksAudioPlayer
import ge.yet.game.miniapp.testkit.MutableMiniAppVisibilitySource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FallingBlocksStorePersistenceTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `terminal state is flushed before topped out label`() = runTest {
        val writer = BarrierWriter()
        val store = FallingBlocksStoreFactory(
            storeFactory = DefaultStoreFactory(),
            engine = TopOutEngine,
            loader = object : GameSnapshotLoader {
                override suspend fun load(): RestoredSession = RestoredSession(gameFixture(), 0, true)
            },
            writer = writer,
            tutorial = SeenTutorial,
            visibility = MutableMiniAppVisibilitySource(),
            seedSource = NewGameSeedSource { 1L },
            audio = NoOpFallingBlocksAudioPlayer,
        ).create()
        val labels = mutableListOf<FallingBlocksStore.Label>()
        val subscription = store.labels(observer(onNext = labels::add))
        advanceUntilIdle()

        store.accept(FallingBlocksStore.Intent.HardDrop)
        runCurrent()

        assertTrue(store.state.game?.phase == GamePhase.TERMINAL)
        assertEquals(emptyList(), labels)
        writer.release.complete(Unit)
        advanceUntilIdle()
        val expected: List<FallingBlocksStore.Label> = listOf(FallingBlocksStore.Label.ToppedOut(1))
        assertEquals(expected, labels)

        subscription.dispose()
        store.dispose()
    }

    @Test
    fun `failed terminal flush still publishes stable topped out label`() = runTest {
        val store = FallingBlocksStoreFactory(
            storeFactory = DefaultStoreFactory(),
            engine = TopOutEngine,
            loader = object : GameSnapshotLoader {
                override suspend fun load(): RestoredSession = RestoredSession(gameFixture(), 0, true)
            },
            writer = object : GameCommitWriter {
                override suspend fun write(state: FallingBlocksState): CommitResult = CommitResult.Failed
            },
            tutorial = SeenTutorial,
            visibility = MutableMiniAppVisibilitySource(),
            seedSource = NewGameSeedSource { 1L },
            audio = NoOpFallingBlocksAudioPlayer,
        ).create()
        val labels = mutableListOf<FallingBlocksStore.Label>()
        val subscription = store.labels(observer(onNext = labels::add))
        advanceUntilIdle()

        store.accept(FallingBlocksStore.Intent.HardDrop)
        advanceUntilIdle()

        assertEquals(GamePhase.TERMINAL, store.state.game?.phase)
        assertEquals(1, labels.size)
        subscription.dispose()
        store.dispose()
    }
}

private object TopOutEngine : FallingBlocksEngine {
    override fun initial(seed: Long, runId: Long): FallingBlocksState =
        DefaultFallingBlocksEngine.initial(seed, runId)

    override fun reduce(state: FallingBlocksState, action: GameAction): GameTransition = GameTransition(
        state = state.copy(phase = GamePhase.TERMINAL),
        facts = listOf(GameFact.Locked, GameFact.ToppedOut),
    )
}

private class BarrierWriter : GameCommitWriter {
    val release = CompletableDeferred<Unit>()
    override suspend fun write(state: FallingBlocksState): CommitResult {
        release.await()
        return CommitResult.Stored
    }
}

private object SeenTutorial : TutorialSeenRepository {
    override suspend fun isTutorialSeen(): Boolean = true
    override suspend fun markTutorialSeen() = Unit
}
