package ge.yet.game.fallingblocks

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import ge.yet.game.miniapp.compose.MiniAppSessionContext
import ge.yet.game.miniapp.metro.MiniAppSessionScope
import ge.yet.game.fallingblocks.component.root.DefaultRootComponent
import ge.yet.game.fallingblocks.component.root.RootComponent

@GraphExtension(MiniAppSessionScope::class)
interface FallingblocksSessionGraph {
    val session: FallingblocksSession

    @Provides @SingleIn(MiniAppSessionScope::class)
    fun provideComponent(componentContext: ComponentContext): RootComponent = DefaultRootComponent(componentContext)

    @Provides @SingleIn(MiniAppSessionScope::class)
    fun provideSession(component: RootComponent): FallingblocksSession = FallingblocksSession(component)

    @ContributesTo(AppScope::class)
    @GraphExtension.Factory
    fun interface Factory {
        fun createGameFallingblocksSessionGraph(
            @Provides context: MiniAppSessionContext,
        ): FallingblocksSessionGraph
    }
}
