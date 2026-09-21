package ge.yet.game.fallingblocks

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import ge.yet.game.fallingblocks.di.FallingblocksSessionGraph
import ge.yet.game.miniapp.api.MiniAppCategoryId
import ge.yet.game.miniapp.api.MiniAppId
import ge.yet.game.miniapp.compose.MiniAppManifest
import ge.yet.game.miniapp.compose.MiniAppPlugin
import ge.yet.game.miniapp.compose.MiniAppSession
import ge.yet.game.miniapp.compose.MiniAppSessionContext
import ge.yet.game.miniapp.metro.RetainedMiniAppSession
import ge.yet.game.fallingblocks.generated.resources.Res
import ge.yet.game.fallingblocks.generated.resources.miniapp_description
import ge.yet.game.fallingblocks.generated.resources.miniapp_icon
import ge.yet.game.fallingblocks.generated.resources.miniapp_title

@Inject
@ContributesIntoSet(AppScope::class)
class FallingblocksPlugin(
    private val graphFactory: FallingblocksSessionGraph.Factory,
) : MiniAppPlugin {
    override val manifest = MiniAppManifest(
        id = MiniAppId("game.fallingblocks"), title = Res.string.miniapp_title, description = Res.string.miniapp_description,
        icon = Res.drawable.miniapp_icon, category = MiniAppCategoryId("game"), sortPriority = 0,
    )
    override fun createSession(context: MiniAppSessionContext): MiniAppSession {
        val graph = graphFactory.createGameFallingblocksSessionGraph(context)
        return RetainedMiniAppSession(graph, graph.session)
    }
}
