package ge.yet.game.fallingblocks

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph
import ge.yet.game.miniapp.api.MiniAppId
import ge.yet.game.miniapp.audio.presets.PlacementClick
import ge.yet.game.miniapp.compose.MiniAppRegistry
import ge.yet.game.miniapp.metro.MiniAppMetroBindings
import ge.yet.game.miniapp.testkit.MiniAppContractAssertions
import ge.yet.game.miniapp.testkit.withMiniAppSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DependencyGraph(
    scope = AppScope::class,
    bindingContainers = [MiniAppMetroBindings::class],
)
interface FallingblocksPluginTestGraph {
    val registry: MiniAppRegistry
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
            MiniAppContractAssertions.assertRetainedGraphSession(session)
            harness.resume()
        }
    }
}
