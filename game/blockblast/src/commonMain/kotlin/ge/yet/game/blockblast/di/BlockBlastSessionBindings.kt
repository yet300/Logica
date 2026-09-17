package ge.yet.game.blockblast.di

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import ge.yet.game.blockblast.component.game.DefaultGameComponentFactory
import ge.yet.game.blockblast.component.game.GameComponent
import ge.yet.game.blockblast.component.result.DefaultGameResultComponentFactory
import ge.yet.game.blockblast.component.result.GameResultComponent
import ge.yet.game.blockblast.data.audio.BlockBlastAudioPlayer
import ge.yet.game.blockblast.data.audio.DefaultBlockBlastAudioPlayer
import ge.yet.game.blockblast.domain.engine.ShapeGenerator
import ge.yet.game.blockblast.domain.engine.WeightedShapeGenerator
import ge.yet.game.domain.repository.FeedbackPreferences
import ge.yet.game.miniapp.api.MiniAppSessionHost
import ge.yet.game.miniapp.api.MiniAppVisibilitySource
import ge.yet.game.miniapp.compose.MiniAppAdsCapability
import ge.yet.game.miniapp.metro.MiniAppSessionScope
import ge.yet.game.blockblast.component.root.DefaultRootComponentFactory
import ge.yet.game.blockblast.component.root.RootComponent
import ge.yet.game.blockblast.session.BlockBlastSession

@BindingContainer
abstract class BlockBlastSessionBindings {
    @Binds
    internal abstract fun bindAudioPlayer(impl: DefaultBlockBlastAudioPlayer): BlockBlastAudioPlayer

    @Binds
    internal abstract fun bindShapeGenerator(impl: WeightedShapeGenerator): ShapeGenerator

    @Binds
    internal abstract fun bindGameComponentFactory(impl: DefaultGameComponentFactory): GameComponent.Factory

    @Binds
    internal abstract fun bindGameResultComponentFactory(
        impl: DefaultGameResultComponentFactory,
    ): GameResultComponent.Factory

    @Binds
    internal abstract fun bindSessionComponentFactory(
        impl: DefaultRootComponentFactory,
    ): RootComponent.Factory

    companion object {
        @Provides
        @SingleIn(MiniAppSessionScope::class)
        internal fun provideRootComponent(
            factory: RootComponent.Factory,
            componentContext: ComponentContext,
            visibility: MiniAppVisibilitySource,
            host: MiniAppSessionHost,
        ): RootComponent = factory.create(componentContext, visibility, host)

        @Provides
        @SingleIn(MiniAppSessionScope::class)
        internal fun provideMiniAppSession(
            component: RootComponent,
            interstitials: MiniAppAdsCapability,
            feedback: FeedbackPreferences,
        ): BlockBlastSession = BlockBlastSession(component, interstitials, feedback)
    }
}
