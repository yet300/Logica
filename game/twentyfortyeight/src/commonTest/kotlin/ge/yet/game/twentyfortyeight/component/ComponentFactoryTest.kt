package ge.yet.game.twentyfortyeight.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import ge.yet.game.miniapp.testkit.NoopMiniAppStorage
import ge.yet.game.twentyfortyeight.component.overlay.DefaultOverlayComponentFactory
import ge.yet.game.twentyfortyeight.component.overlay.OverlayComponent
import ge.yet.game.twentyfortyeight.component.playing.DefaultPlayingComponentFactory
import ge.yet.game.twentyfortyeight.component.playing.PlayingComponent
import ge.yet.game.twentyfortyeight.component.result.DefaultResultComponentFactory
import ge.yet.game.twentyfortyeight.component.result.ResultComponent
import ge.yet.game.twentyfortyeight.domain.model.GameStatistics
import ge.yet.game.twentyfortyeight.domain.model.ResultSnapshot
import ge.yet.game.twentyfortyeight.domain.engine.MoveEngine
import ge.yet.game.twentyfortyeight.domain.engine.SpawnPolicy
import ge.yet.game.twentyfortyeight.domain.repository.GameSnapshotLoader
import ge.yet.game.twentyfortyeight.data.SessionPersistenceCoordinator
import ge.yet.game.twentyfortyeight.session.DefaultTwentyFortyEightSessionComponentFactory
import ge.yet.game.twentyfortyeight.session.TwentyFortyEightSessionAdapter
import ge.yet.game.twentyfortyeight.session.TwentyFortyEightSessionPorts
import ge.yet.game.twentyfortyeight.component.playing.store.ImmediateCommitWriter
import ge.yet.game.twentyfortyeight.component.playing.store.MutableVisibility
import ge.yet.game.twentyfortyeight.component.playing.store.NewGameSeedSource
import ge.yet.game.twentyfortyeight.component.playing.store.TwentyFortyEightStoreFactory
import ge.yet.game.twentyfortyeight.component.playing.store.loaded
import ge.yet.game.twentyfortyeight.component.playing.store.playableGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ComponentFactoryTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `overlay factory snapshots victory model and routes callbacks`() {
        val factory: OverlayComponent.Factory = DefaultOverlayComponentFactory()
        var continued = false
        var restarted = false
        var dismissed = false

        val overlay = assertIs<OverlayComponent.Victory>(
            factory.createVictory(
                score = 2048L,
                bestScore = 4096L,
                onContinue = { continued = true },
                onRestart = { restarted = true },
                onDismiss = { dismissed = true },
            ),
        )

        assertEquals(OverlayComponent.Model.Victory(2048L, 4096L), overlay.model.value)
        overlay.onContinueRequested()
        overlay.onRestartRequested()
        overlay.onDismissRequested()
        assertTrue(continued && restarted && dismissed)
    }

    @Test
    fun `overlay factory snapshots restart confirmation model and routes callbacks`() {
        val factory: OverlayComponent.Factory = DefaultOverlayComponentFactory()
        var confirmed = false
        var dismissed = false

        val overlay = assertIs<OverlayComponent.RestartConfirmation>(
            factory.createRestartConfirmation(
                score = 512L,
                successfulMovesInRun = 42L,
                onConfirm = { confirmed = true },
                onDismiss = { dismissed = true },
            ),
        )

        assertEquals(OverlayComponent.Model.RestartConfirmation(512L, 42L), overlay.model.value)
        overlay.onConfirmRequested()
        overlay.onDismissRequested()
        assertTrue(confirmed && dismissed)
    }

    @Test
    fun `playing factory creates independent components over one store`() {
        val store = testStoreFactory().create()
        val playingFactory: PlayingComponent.Factory =
            DefaultPlayingComponentFactory(DefaultOverlayComponentFactory())
        val firstContext = testComponentContext()
        val secondContext = testComponentContext()

        val firstPlaying = playingFactory.create(firstContext.context, store)
        val secondPlaying = playingFactory.create(secondContext.context, store)
        assertNotSame(firstPlaying, secondPlaying)

        store.dispose()
        firstContext.lifecycle.destroy()
        secondContext.lifecycle.destroy()
    }

    @Test
    fun `result factory builds detached snapshot models and routes new game`() {
        val resultFactory: ResultComponent.Factory = DefaultResultComponentFactory()
        val snapshot = ResultSnapshot(score = 1024L, bestScore = 2048L, highestTile = 512L, statistics = GameStatistics())

        var firstNewGames = 0
        var secondNewGames = 0
        val firstResult = resultFactory.create(snapshot) { firstNewGames++ }
        val secondResult = resultFactory.create(snapshot) { secondNewGames++ }
        assertNotSame(firstResult, secondResult)

        assertEquals(ResultComponent.Model(1024L, 2048L, 512L), firstResult.model.value)
        firstResult.onNewGameRequested()
        assertEquals(1, firstNewGames)
        assertEquals(0, secondNewGames)
    }

    @Test
    fun `session factory creates independent session components`() {
        val ports = TwentyFortyEightSessionPorts()
        val factory: ge.yet.game.twentyfortyeight.session.TwentyFortyEightSessionComponent.Factory =
            DefaultTwentyFortyEightSessionComponentFactory(
                playingFactory = DefaultPlayingComponentFactory(DefaultOverlayComponentFactory()),
                resultFactory = DefaultResultComponentFactory(),
                storeFactory = testStoreFactory(),
                adapter = TwentyFortyEightSessionAdapter(
                    navigation = ports,
                    audio = ge.yet.game.twentyfortyeight.audio.TwentyFortyEightAudioAdapter(
                        ge.yet.game.miniapp.testkit.NoopMiniAppAudio,
                    ),
                    analytics = ge.yet.game.twentyfortyeight.analytics.TwentyFortyEightAnalytics(
                        NoOpAnalytics,
                    ),
                    diagnostics = ge.yet.game.twentyfortyeight.diagnostics.TwentyFortyEightDiagnostics {},
                    host = NoOpHost,
                    uiEffects = ports,
                ),
                ports = ports,
            )

        val firstSessionContext = testComponentContext()
        val secondSessionContext = testComponentContext()
        val first = factory.create(firstSessionContext.context)
        val second = factory.create(secondSessionContext.context)
        assertNotSame(first, second)
        firstSessionContext.lifecycle.destroy()
        secondSessionContext.lifecycle.destroy()
    }

    private fun testStoreFactory(): TwentyFortyEightStoreFactory {
        val coordinator = SessionPersistenceCoordinator(
            NoopMiniAppStorage,
            ImmediateCommitWriter(),
            GameSnapshotLoader { loaded(game = playableGame()) },
        )
        return TwentyFortyEightStoreFactory(
            storeFactory = DefaultStoreFactory(),
            engine = MoveEngine(SpawnPolicy()),
            coordinator = coordinator,
            visibility = MutableVisibility(),
            seedSource = NewGameSeedSource { 0x2048L },
        )
    }

    private fun testComponentContext(): TestContext {
        val lifecycle = LifecycleRegistry()
        return TestContext(
            context = DefaultComponentContext(lifecycle = lifecycle),
            lifecycle = lifecycle,
        )
    }

    private data class TestContext(
        val context: DefaultComponentContext,
        val lifecycle: LifecycleRegistry,
    )

    private data object NoOpAnalytics : ge.yet.game.domain.repository.AnalyticRepository {
        override fun logEvent(eventName: String, params: Map<String, Any>?) = Unit
        override fun deleteData() = Unit
    }

    private data object NoOpHost : ge.yet.game.miniapp.api.MiniAppSessionHost {
        override fun close() = Unit
        override fun requestReview(opportunity: ge.yet.game.miniapp.api.MiniAppReviewOpportunity) = Unit
    }
}
