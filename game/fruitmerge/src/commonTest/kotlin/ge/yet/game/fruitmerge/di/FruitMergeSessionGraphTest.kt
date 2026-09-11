package ge.yet.game.fruitmerge.di

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraph
import ge.yet.game.fruitmerge.FruitMergeGraphTestBindings
import ge.yet.game.fruitmerge.FruitMergeSession
import ge.yet.game.fruitmerge.FruitMergeSessionBindings
import ge.yet.game.fruitmerge.audio.FruitMergeAudioAdapter
import ge.yet.game.fruitmerge.engine.FruitMergeEngine
import ge.yet.game.fruitmerge.engine.FruitMergeRules
import ge.yet.game.fruitmerge.engine.FruitPhysics
import ge.yet.game.fruitmerge.persistence.FruitMergePersistence
import ge.yet.game.fruitmerge.session.DefaultFruitMergeSessionComponent
import ge.yet.game.fruitmerge.session.FruitMergeSessionComponent
import ge.yet.game.fruitmerge.store.FruitMergeStore
import ge.yet.game.miniapp.api.MiniAppSessionHost
import ge.yet.game.miniapp.api.MiniAppStorage
import ge.yet.game.miniapp.api.MiniAppVisibilitySource
import ge.yet.game.miniapp.audio.MiniAppAudio
import ge.yet.game.miniapp.compose.MiniAppSessionContext
import ge.yet.game.miniapp.metro.MiniAppMetroBindings
import ge.yet.game.miniapp.metro.MiniAppSessionScope
import ge.yet.game.miniapp.testkit.MiniAppLifecycleHarness
import ge.yet.game.miniapp.testkit.MutableMiniAppStorage
import ge.yet.game.miniapp.testkit.MutableMiniAppVisibilitySource
import ge.yet.game.miniapp.testkit.NoopMiniAppAudio
import ge.yet.game.miniapp.testkit.RecordingMiniAppSessionHost
import ge.yet.game.miniapp.testkit.TestMiniAppSessionContext
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@DependencyGraph(
    scope = AppScope::class,
    bindingContainers = [
        MiniAppMetroBindings::class,
        FruitMergeGraphTestBindings::class,
    ],
)
internal interface InspectableFruitMergeAppGraph {
    val sessionFactory: InspectableFruitMergeSessionGraph.Factory
}

@GraphExtension(
    scope = MiniAppSessionScope::class,
    bindingContainers = [FruitMergeSessionBindings::class],
)
internal interface InspectableFruitMergeSessionGraph {
    val session: FruitMergeSession
    val component: FruitMergeSessionComponent
    val concreteComponent: DefaultFruitMergeSessionComponent
    val store: FruitMergeStore
    val engine: FruitMergeEngine
    val rules: FruitMergeRules
    val physics: FruitPhysics
    val persistence: FruitMergePersistence
    val audioAdapter: FruitMergeAudioAdapter
    val componentContext: ComponentContext
    val visibility: MiniAppVisibilitySource
    val host: MiniAppSessionHost
    val storage: MiniAppStorage
    val audio: MiniAppAudio
    val context: MiniAppSessionContext

    @ContributesTo(AppScope::class)
    @GraphExtension.Factory
    fun interface Factory {
        fun createInspectableFruitMergeSessionGraph(
            @Provides context: MiniAppSessionContext,
        ): InspectableFruitMergeSessionGraph
    }
}

class FruitMergeSessionGraphTest {
    @Test
    fun `two child graphs isolate session state and share the rules alias`() {
        val app = createGraph<InspectableFruitMergeAppGraph>()
        val firstLifecycle = MiniAppLifecycleHarness().also { it.resume() }
        val secondLifecycle = MiniAppLifecycleHarness().also { it.resume() }
        val firstStorage = MutableMiniAppStorage()
        val secondStorage = MutableMiniAppStorage()
        val firstContext = TestMiniAppSessionContext(
            firstLifecycle.componentContext,
            MutableMiniAppVisibilitySource(),
            RecordingMiniAppSessionHost(),
            firstStorage,
            NoopMiniAppAudio,
        )
        val secondContext = TestMiniAppSessionContext(
            secondLifecycle.componentContext,
            MutableMiniAppVisibilitySource(),
            RecordingMiniAppSessionHost(),
            secondStorage,
            NoopMiniAppAudio,
        )

        val first = app.sessionFactory.createInspectableFruitMergeSessionGraph(firstContext)
        val second = app.sessionFactory.createInspectableFruitMergeSessionGraph(secondContext)

        assertSame(first.component, first.concreteComponent)
        assertSame(first.store, first.concreteComponent.retainedStore)
        assertSame(first.concreteComponent.game, first.concreteComponent.gameComponent)
        assertNotSame(first.session, second.session)
        assertNotSame(first.component, second.component)
        assertNotSame(first.concreteComponent.game, second.concreteComponent.game)
        assertNotSame(first.store, second.store)
        assertNotSame(first.audioAdapter, second.audioAdapter)
        assertNotSame(first.engine, second.engine)
        assertNotSame(first.physics, second.physics)
        // Stateless persistence is unscoped: no identity is shared across sessions.
        assertNotSame(first.persistence, second.persistence)
        // The unscoped alias resolves to the session-scoped engine.
        assertSame(first.engine, first.rules)
        assertSame(second.engine, second.rules)

        assertSame(firstContext.componentContext, first.componentContext)
        assertSame(firstContext, first.context)
        assertSame(firstContext.visibility, first.visibility)
        assertSame(firstContext.host, first.host)
        assertSame(firstContext.storage, first.storage)
        assertSame(firstContext.audio, first.audio)
        assertSame(firstStorage, first.storage)
        assertSame(secondStorage, second.storage)
        assertSame(secondContext, second.context)
        assertNotSame(first.storage, second.storage)
        assertNotSame(first.visibility, second.visibility)
        assertNotSame(first.host, second.host)

        firstLifecycle.destroy()
        secondLifecycle.destroy()
        first.store.dispose()
        second.store.dispose()
    }
}
