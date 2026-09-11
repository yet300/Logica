package ge.yet.game.fruitmerge.session

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import ge.yet.game.fruitmerge.TestFruitMergeRules
import ge.yet.game.fruitmerge.audio.FruitMergeAudioAdapter
import ge.yet.game.fruitmerge.data.FruitMergePersistence
import ge.yet.game.fruitmerge.session.store.FruitMergeStoreFactory
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
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame

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
    fun `game factory creates distinct children over one retained store`() = runTest {
        val storage = MutableMiniAppStorage()
        val persistence = FruitMergePersistence(storage)
        val visibility = MutableMiniAppVisibilitySource()
        val lifecycle = MiniAppLifecycleHarness().also { it.resume() }
        val store = FruitMergeStoreFactory(
            storeFactory = DefaultStoreFactory(),
            rules = TestFruitMergeRules(),
            snapshotLoader = persistence,
            commitWriter = persistence,
        ).create()
        val factory: FruitMergeComponent.Factory =
            DefaultFruitMergeComponentFactory(persistence, visibility)

        val first = factory.create(lifecycle.componentContext, store)
        val second = factory.create(lifecycle.componentContext, store)

        assertIs<DefaultFruitMergeComponent>(first)
        assertIs<DefaultFruitMergeComponent>(second)
        assertNotSame(first, second)
        lifecycle.destroy()
        store.dispose()
    }

    @Test
    fun `session factory assembles retained store and game child`() = runTest {
        val storage = MutableMiniAppStorage()
        val persistence = FruitMergePersistence(storage)
        val visibility = MutableMiniAppVisibilitySource()
        val lifecycle = MiniAppLifecycleHarness().also { it.resume() }
        val storeFactory = FruitMergeStoreFactory(
            storeFactory = DefaultStoreFactory(),
            rules = TestFruitMergeRules(),
            snapshotLoader = persistence,
            commitWriter = persistence,
        )
        val factory: FruitMergeSessionComponent.Factory = DefaultFruitMergeSessionComponentFactory(
            gameFactory = DefaultFruitMergeComponentFactory(persistence, visibility),
            storeFactory = storeFactory,
            audio = FruitMergeAudioAdapter(NoopMiniAppAudio),
        )

        val session = factory.create(lifecycle.componentContext)

        assertIs<DefaultFruitMergeSessionComponent>(session)
        advanceUntilIdle()
        assertSame(session.gameComponent, session.game)
        kotlin.test.assertTrue(session.retainedStore.state.initialized)
        lifecycle.destroy()
        session.retainedStore.dispose()
    }
}
