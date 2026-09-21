package ge.yet.game.fallingblocks

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph
import ge.yet.game.miniapp.api.MiniAppId
import ge.yet.game.miniapp.audio.presets.PlacementClick
import ge.yet.game.miniapp.compose.MiniAppRegistry
import ge.yet.game.miniapp.compose.MiniAppAdGate
import ge.yet.game.miniapp.compose.MiniAppAdKind
import ge.yet.game.miniapp.compose.MiniAppAdsCapability
import ge.yet.game.miniapp.compose.MiniAppFrameMode
import ge.yet.game.miniapp.metro.MiniAppMetroBindings
import ge.yet.game.miniapp.testkit.MiniAppContractAssertions
import ge.yet.game.miniapp.testkit.withMiniAppSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DependencyGraph(
    scope = AppScope::class,
    bindingContainers = [MiniAppMetroBindings::class, FallingblocksPluginTestBindings::class],
)
interface FallingblocksPluginTestGraph {
    val registry: MiniAppRegistry
}

@BindingContainer
object FallingblocksPluginTestBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideAds(): MiniAppAdsCapability = object : MiniAppAdsCapability {
        @Composable
        override fun rememberGate(kind: MiniAppAdKind): MiniAppAdGate =
            MiniAppAdGate(willShowAd = false) { complete -> complete() }
    }
}

class FallingblocksPluginContractTest {
    @Test
    fun `isolated graph contains exactly this plugin`() {
        val expectedId = MiniAppId("game.fallingblocks")
        val graph = createGraph<FallingblocksPluginTestGraph>()

        MiniAppContractAssertions.assertSinglePlugin(graph.registry, expectedId)
        val plugin = assertNotNull(graph.registry[expectedId])
        MiniAppContractAssertions.assertManifest(plugin, expectedId)
    }

    @Test
    fun `plugin creates a graph retained session`() {
        val expectedId = MiniAppId("game.fallingblocks")
        val graph = createGraph<FallingblocksPluginTestGraph>()
        val plugin = assertNotNull(graph.registry[expectedId])
        withMiniAppSession { harness ->
            val sharedSfx = PlacementClick()
            assertNotNull(harness.context.audio)
            assertNotNull(sharedSfx)
            val session = plugin.createSession(harness.context)
            assertEquals(expectedId, plugin.manifest.id)
            assertTrue(session.wantsBanner)
            assertEquals(MiniAppFrameMode.Standard, session.frameMode.value)
            MiniAppContractAssertions.assertRetainedGraphSession(session)
            harness.resume()
        }
    }
}
