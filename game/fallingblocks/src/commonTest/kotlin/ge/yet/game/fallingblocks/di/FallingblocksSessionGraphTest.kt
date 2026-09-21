package ge.yet.game.fallingblocks.di

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraph
import ge.yet.game.fallingblocks.FallingblocksPluginTestBindings
import ge.yet.game.fallingblocks.FallingblocksSession
import ge.yet.game.fallingblocks.FallingblocksSessionBindings
import ge.yet.game.fallingblocks.audio.FallingBlocksAudioAdapter
import ge.yet.game.fallingblocks.component.root.RootComponent
import ge.yet.game.fallingblocks.data.FallingBlocksPersistence
import ge.yet.game.fallingblocks.domain.model.FallingBlocksEngine
import ge.yet.game.miniapp.api.MiniAppId
import ge.yet.game.miniapp.api.MiniAppSessionHost
import ge.yet.game.miniapp.api.MiniAppStorage
import ge.yet.game.miniapp.api.MiniAppVisibility
import ge.yet.game.miniapp.api.MiniAppVisibilitySource
import ge.yet.game.miniapp.audio.MiniAppAudio
import ge.yet.game.miniapp.compose.MiniAppSessionContext
import ge.yet.game.miniapp.compose.MiniAppRegistry
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
import kotlin.test.assertNotNull
import kotlin.test.assertSame

@DependencyGraph(
    scope = AppScope::class,
    bindingContainers = [MiniAppMetroBindings::class, FallingblocksPluginTestBindings::class],
)
internal interface InspectableFallingblocksAppGraph {
    val factory: InspectableFallingblocksSessionGraph.Factory
    val registry: MiniAppRegistry
}

@GraphExtension(
    scope = MiniAppSessionScope::class,
    bindingContainers = [FallingblocksSessionBindings::class],
)
internal interface InspectableFallingblocksSessionGraph {
    val session: FallingblocksSession
    val component: RootComponent
    val persistence: FallingBlocksPersistence
    val engine: FallingBlocksEngine
    val audioAdapter: FallingBlocksAudioAdapter
    val componentContext: ComponentContext
    val visibility: MiniAppVisibilitySource
    val host: MiniAppSessionHost
    val storage: MiniAppStorage
    val audio: MiniAppAudio
    val context: MiniAppSessionContext

    @GraphExtension.Factory
    fun interface Factory {
        fun createInspectableFallingblocksSessionGraph(
            @Provides context: MiniAppSessionContext,
        ): InspectableFallingblocksSessionGraph
    }
}

class FallingblocksSessionGraphTest {
    @Test
    fun `child graph retains one isolated set of session scoped objects`() {
        val app = createGraph<InspectableFallingblocksAppGraph>()
        assertNotNull(app.registry[MiniAppId("game.fallingblocks")])
        val firstLifecycle = MiniAppLifecycleHarness().also { it.resume() }
        val secondLifecycle = MiniAppLifecycleHarness().also { it.resume() }
        val firstContext = context(firstLifecycle)
        val secondContext = context(secondLifecycle)

        val first = app.factory.createInspectableFallingblocksSessionGraph(firstContext)
        val second = app.factory.createInspectableFallingblocksSessionGraph(secondContext)

        assertSame(first.session, first.session)
        assertSame(first.component, first.component)
        assertSame(first.audioAdapter, first.audioAdapter)
        assertSame(firstContext, first.context)
        assertSame(firstContext.componentContext, first.componentContext)
        assertSame(firstContext.visibility, first.visibility)
        assertSame(firstContext.host, first.host)
        assertSame(firstContext.storage, first.storage)
        assertSame(firstContext.audio, first.audio)

        assertNotSame(first.session, second.session)
        assertNotSame(first.component, second.component)
        assertNotSame(first.audioAdapter, second.audioAdapter)
        assertNotSame(first.persistence, second.persistence)
        assertNotSame(first.storage, second.storage)
        assertNotSame(first.visibility, second.visibility)

        firstLifecycle.destroy()
        secondLifecycle.destroy()
    }

    private fun context(lifecycle: MiniAppLifecycleHarness): TestMiniAppSessionContext =
        TestMiniAppSessionContext(
            componentContext = lifecycle.componentContext,
            visibility = MutableMiniAppVisibilitySource(MiniAppVisibility.OBSCURED),
            host = RecordingMiniAppSessionHost(),
            storage = MutableMiniAppStorage(),
            audio = NoopMiniAppAudio,
        )
}
