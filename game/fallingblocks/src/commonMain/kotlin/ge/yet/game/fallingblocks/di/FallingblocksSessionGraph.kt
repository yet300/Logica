package ge.yet.game.fallingblocks.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import ge.yet.game.fallingblocks.FallingblocksSession
import ge.yet.game.miniapp.compose.MiniAppSessionContext
import ge.yet.game.miniapp.metro.MiniAppSessionScope

@GraphExtension(
    scope = MiniAppSessionScope::class,
    bindingContainers = [FallingblocksSessionBindings::class],
)
interface FallingblocksSessionGraph {
    val session: FallingblocksSession

    @ContributesTo(AppScope::class)
    @GraphExtension.Factory
    fun interface Factory {
        fun createGameFallingblocksSessionGraph(
            @Provides context: MiniAppSessionContext,
        ): FallingblocksSessionGraph
    }
}
