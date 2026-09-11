package ge.yet.game.fruitmerge.component

import com.arkivanov.decompose.childContext
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import ge.yet.game.fruitmerge.TestFruitMergeRules
import ge.yet.game.fruitmerge.audio.FruitMergeAudioAdapter
import ge.yet.game.fruitmerge.component.game.DefaultFruitMergeComponent
import ge.yet.game.fruitmerge.component.game.DefaultFruitMergeComponentFactory
import ge.yet.game.fruitmerge.component.game.FruitMergeComponent
import ge.yet.game.fruitmerge.component.game.store.FruitMergeStoreFactory
import ge.yet.game.fruitmerge.component.result.DefaultFruitMergeResultComponent
import ge.yet.game.fruitmerge.component.result.DefaultFruitMergeResultComponentFactory
import ge.yet.game.fruitmerge.component.result.FruitMergeResultComponent
import ge.yet.game.fruitmerge.component.result.FruitMergeResultSnapshot
import ge.yet.game.fruitmerge.component.session.DefaultFruitMergeSessionComponent
import ge.yet.game.fruitmerge.component.session.DefaultFruitMergeSessionComponentFactory
import ge.yet.game.fruitmerge.component.session.FruitMergeSessionComponent
import ge.yet.game.fruitmerge.data.FruitMergePersistence
import ge.yet.game.fruitmerge.domain.model.FruitLevel
import ge.yet.game.miniapp.testkit.MiniAppLifecycleHarness
import ge.yet.game.miniapp.testkit.MutableMiniAppStorage
import ge.yet.game.miniapp.testkit.MutableMiniAppVisibilitySource
import ge.yet.game.miniapp.testkit.NoopMiniAppAudio
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
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
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
    fun `game factory retains a store per child and routes completion`() = runTest {
        val storage = MutableMiniAppStorage()
        val persistence = FruitMergePersistence(storage)
        val visibility = MutableMiniAppVisibilitySource()
        val lifecycle = MiniAppLifecycleHarness().also { it.resume() }
        val factory: FruitMergeComponent.Factory = DefaultFruitMergeComponentFactory(
            gameStoreFactory = FruitMergeStoreFactory(
                storeFactory = DefaultStoreFactory(),
                rules = TestFruitMergeRules(),
                snapshotLoader = persistence,
                commitWriter = persistence,
            ),
            audio = FruitMergeAudioAdapter(NoopMiniAppAudio),
            tutorial = persistence,
            visibility = visibility,
        )
        var completions = 0
        // Sibling children own distinct instance keepers, hence distinct retained stores.
        val first = factory.create(
            lifecycle.componentContext.childContext(key = "first"),
            isNewGame = false,
        ) { completions += 1 }
        val second = factory.create(
            lifecycle.componentContext.childContext(key = "second"),
            isNewGame = true,
        ) { completions += 1 }

        assertIs<DefaultFruitMergeComponent>(first)
        assertIs<DefaultFruitMergeComponent>(second)
        assertNotSame(first, second)
        advanceUntilIdle()
        assertTrue(first.model.value.initialized)
        assertNotSame(first.store, second.store)
        assertEquals(0, completions)
        lifecycle.destroy()
        first.store.dispose()
        second.store.dispose()
    }

    @Test
    fun `result factory snapshots the model and routes new game`() {
        val factory: FruitMergeResultComponent.Factory = DefaultFruitMergeResultComponentFactory()
        val snapshot = FruitMergeResultSnapshot(
            score = 1_250L,
            bestScore = 2_000L,
            bestImprovedInRun = true,
            largestFruit = FruitLevel.APPLE,
            runOrdinal = 3L,
        )
        var newGames = 0
        val lifecycle = MiniAppLifecycleHarness().also { it.resume() }

        val component = factory.create(lifecycle.componentContext, snapshot) { newGames += 1 }

        assertIs<DefaultFruitMergeResultComponent>(component)
        assertEquals(snapshot, component.model.value.snapshot)
        component.onNewGame()
        assertEquals(1, newGames)
        lifecycle.destroy()
    }

    @Test
    fun `session factory opens on the playing child`() = runTest {
        val storage = MutableMiniAppStorage()
        val persistence = FruitMergePersistence(storage)
        val visibility = MutableMiniAppVisibilitySource()
        val lifecycle = MiniAppLifecycleHarness().also { it.resume() }
        val factory: FruitMergeSessionComponent.Factory = DefaultFruitMergeSessionComponentFactory(
            gameFactory = DefaultFruitMergeComponentFactory(
                gameStoreFactory = FruitMergeStoreFactory(
                    storeFactory = DefaultStoreFactory(),
                    rules = TestFruitMergeRules(),
                    snapshotLoader = persistence,
                    commitWriter = persistence,
                ),
                audio = FruitMergeAudioAdapter(NoopMiniAppAudio),
                tutorial = persistence,
                visibility = visibility,
            ),
            resultFactory = DefaultFruitMergeResultComponentFactory(),
        )

        val session = factory.create(lifecycle.componentContext)

        assertIs<DefaultFruitMergeSessionComponent>(session)
        advanceUntilIdle()
        assertSame(session.gameComponent, session.game)
        assertIs<FruitMergeSessionComponent.Child.Playing>(session.stack.value.active.instance)
        lifecycle.destroy()
        session.gameComponent.store.dispose()
    }
}
