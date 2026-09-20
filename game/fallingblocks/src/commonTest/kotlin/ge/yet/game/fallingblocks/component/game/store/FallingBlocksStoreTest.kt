package ge.yet.game.fallingblocks.component.game.store

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import ge.yet.game.fallingblocks.domain.model.FallingBlocksEngine
import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.domain.model.GameAction
import ge.yet.game.fallingblocks.domain.model.GameTransition
import ge.yet.game.fallingblocks.domain.repository.CommitResult
import ge.yet.game.fallingblocks.domain.repository.GameCommitWriter
import ge.yet.game.fallingblocks.domain.repository.GameSnapshotLoader
import ge.yet.game.fallingblocks.domain.repository.RestoredSession
import ge.yet.game.fallingblocks.domain.repository.TutorialSeenRepository
import ge.yet.game.fallingblocks.gameFixture
import ge.yet.game.fallingblocks.NoOpFallingBlocksAudioPlayer
import ge.yet.game.miniapp.api.MiniAppVisibility
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FallingBlocksStoreTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `fresh bootstrap ignores input until loading completes`() = runTest {
        val loader = ControlledLoader()
        val engine = RecordingEngine()
        val store = createStore(engine = engine, loader = loader)
        runCurrent()

        store.accept(FallingBlocksStore.Intent.Move(1))
        runCurrent()
        assertTrue(store.state.loading)
        assertEquals(emptyList(), engine.actions)

        loader.result.complete(RestoredSession(state = null, bestScore = 11, tutorialSeen = false))
        advanceUntilIdle()

        assertFalse(store.state.loading)
        assertEquals(11, store.state.bestScore)
        assertEquals(engine.initialState, store.state.game)
        store.dispose()
    }

    @Test
    fun `bootstrap restores exact playable or terminal state`() = runTest {
        val playable = gameFixture(score = 42)
        val terminal = gameFixture(score = 99).copy(phase = ge.yet.game.fallingblocks.domain.model.GamePhase.TERMINAL)

        val playingStore = createStore(loader = ImmediateLoader(RestoredSession(playable, 100, true)))
        val terminalStore = createStore(loader = ImmediateLoader(RestoredSession(terminal, 120, true)))
        advanceUntilIdle()

        assertEquals(playable, playingStore.state.game)
        assertEquals(terminal, terminalStore.state.game)
        assertTrue(terminalStore.state.tutorialSeen)
        playingStore.dispose()
        terminalStore.dispose()
    }

    @Test
    fun `frames are rejected while visibility is not active`() = runTest {
        val visibility = MutableMiniAppVisibilitySource()
        val engine = RecordingEngine()
        val store = createStore(engine = engine, visibility = visibility)
        advanceUntilIdle()

        visibility.set(MiniAppVisibility.OBSCURED)
        runCurrent()
        store.accept(FallingBlocksStore.Intent.Frame(16))
        runCurrent()

        assertEquals(emptyList(), engine.actions)
        assertFalse(store.state.active)
        store.dispose()
    }

    @Test
    fun `leaving active visibility checkpoints once per transition`() = runTest {
        val visibility = MutableMiniAppVisibilitySource()
        val writer = RecordingWriter()
        val store = createStore(writer = writer, visibility = visibility)
        advanceUntilIdle()

        visibility.set(MiniAppVisibility.OBSCURED)
        runCurrent()
        visibility.set(MiniAppVisibility.BACKGROUND)
        advanceUntilIdle()

        assertEquals(1, writer.states.size)
        store.dispose()
    }

    @Test
    fun `rapid intents reach the engine in order`() = runTest {
        val engine = RecordingEngine()
        val store = createStore(engine = engine)
        advanceUntilIdle()

        store.accept(FallingBlocksStore.Intent.Move(-1))
        store.accept(FallingBlocksStore.Intent.Rotate)
        store.accept(FallingBlocksStore.Intent.SoftDrop(2))
        runCurrent()

        assertEquals(
            listOf(
                GameAction.MoveHorizontal(-1),
                GameAction.RotateClockwise,
                GameAction.SoftDrop(2),
            ),
            engine.actions,
        )
        store.dispose()
    }

    private fun createStore(
        engine: FallingBlocksEngine = RecordingEngine(),
        loader: GameSnapshotLoader = ImmediateLoader(RestoredSession(null, 0, false)),
        writer: GameCommitWriter = ImmediateWriter,
        visibility: MutableMiniAppVisibilitySource = MutableMiniAppVisibilitySource(),
    ): FallingBlocksStore = FallingBlocksStoreFactory(
        storeFactory = DefaultStoreFactory(),
        engine = engine,
        loader = loader,
        writer = writer,
        tutorial = NoopTutorial,
        visibility = visibility,
        seedSource = NewGameSeedSource { 7L },
        audio = NoOpFallingBlocksAudioPlayer,
    ).create()
}

private class ControlledLoader : GameSnapshotLoader {
    val result = CompletableDeferred<RestoredSession>()
    override suspend fun load(): RestoredSession = result.await()
}

private class ImmediateLoader(private val restored: RestoredSession) : GameSnapshotLoader {
    override suspend fun load(): RestoredSession = restored
}

private object ImmediateWriter : GameCommitWriter {
    override suspend fun write(state: FallingBlocksState): CommitResult = CommitResult.Stored
}

private class RecordingWriter : GameCommitWriter {
    val states = mutableListOf<FallingBlocksState>()
    override suspend fun write(state: FallingBlocksState): CommitResult {
        states += state
        return CommitResult.Stored
    }
}

private object NoopTutorial : TutorialSeenRepository {
    override suspend fun isTutorialSeen(): Boolean = false
    override suspend fun markTutorialSeen() = Unit
}

private class RecordingEngine : FallingBlocksEngine {
    val initialState = gameFixture()
    val actions = mutableListOf<GameAction>()

    override fun initial(seed: Long, runId: Long): FallingBlocksState = initialState.copy(runId = runId)

    override fun reduce(state: FallingBlocksState, action: GameAction): GameTransition {
        actions += action
        return GameTransition(state.copy(score = state.score + 1), emptyList())
    }
}
