package ge.yet.game.twentyfortyeight.di

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import ge.yet.game.miniapp.metro.MiniAppSessionScope
import ge.yet.game.twentyfortyeight.TwentyFortyEightSession
import ge.yet.game.twentyfortyeight.component.overlay.DefaultOverlayComponentFactory
import ge.yet.game.twentyfortyeight.component.overlay.OverlayComponent
import ge.yet.game.twentyfortyeight.component.playing.DefaultPlayingComponentFactory
import ge.yet.game.twentyfortyeight.component.playing.PlayingComponent
import ge.yet.game.twentyfortyeight.component.playing.store.NewGameSeedSource
import ge.yet.game.twentyfortyeight.component.playing.store.RandomNewGameSeedSource
import ge.yet.game.twentyfortyeight.component.playing.store.TwentyFortyEightStore
import ge.yet.game.twentyfortyeight.component.result.DefaultResultComponentFactory
import ge.yet.game.twentyfortyeight.component.result.ResultComponent
import ge.yet.game.twentyfortyeight.domain.repository.GameCommitWriter
import ge.yet.game.twentyfortyeight.domain.repository.GameSnapshotLoader
import ge.yet.game.twentyfortyeight.data.TwentyFortyEightPersistence
import ge.yet.game.twentyfortyeight.session.DefaultTwentyFortyEightSessionComponent
import ge.yet.game.twentyfortyeight.session.DefaultTwentyFortyEightSessionComponentFactory
import ge.yet.game.twentyfortyeight.session.SessionNavigation
import ge.yet.game.twentyfortyeight.session.SessionUiEffects
import ge.yet.game.twentyfortyeight.session.TwentyFortyEightSessionComponent
import ge.yet.game.twentyfortyeight.session.TwentyFortyEightSessionPorts

@BindingContainer
abstract class TwentyFortyEightSessionBindings {
    @Binds
    internal abstract fun bindNavigation(ports: TwentyFortyEightSessionPorts): SessionNavigation

    @Binds
    internal abstract fun bindUiEffects(ports: TwentyFortyEightSessionPorts): SessionUiEffects

    // The app-scoped persistence plays both roles in the session coordinator.
    @Binds
    internal abstract fun bindCommitWriter(persistence: TwentyFortyEightPersistence): GameCommitWriter

    @Binds
    internal abstract fun bindSnapshotLoader(persistence: TwentyFortyEightPersistence): GameSnapshotLoader

    @Binds
    internal abstract fun bindComponentContract(
        component: DefaultTwentyFortyEightSessionComponent,
    ): TwentyFortyEightSessionComponent

    @Binds
    internal abstract fun bindOverlayComponentFactory(
        impl: DefaultOverlayComponentFactory,
    ): OverlayComponent.Factory

    @Binds
    internal abstract fun bindPlayingComponentFactory(
        impl: DefaultPlayingComponentFactory,
    ): PlayingComponent.Factory

    @Binds
    internal abstract fun bindResultComponentFactory(
        impl: DefaultResultComponentFactory,
    ): ResultComponent.Factory

    @Binds
    internal abstract fun bindSessionComponentFactory(
        impl: DefaultTwentyFortyEightSessionComponentFactory,
    ): TwentyFortyEightSessionComponent.Factory

    @Binds
    internal abstract fun bindSeedSource(impl: RandomNewGameSeedSource): NewGameSeedSource

    companion object {
        @Provides
        @SingleIn(MiniAppSessionScope::class)
        internal fun provideComponent(
            factory: TwentyFortyEightSessionComponent.Factory,
            componentContext: ComponentContext,
        ): DefaultTwentyFortyEightSessionComponent =
            // Safe: the only Factory binding in this scope is DefaultTwentyFortyEightSessionComponentFactory,
            // whose create() returns the Default type (covariant override). Concrete type is kept
            // because provideStore exposes the retained store for graph inspection.
            factory.create(componentContext) as DefaultTwentyFortyEightSessionComponent

        @Provides
        @SingleIn(MiniAppSessionScope::class)
        internal fun provideStore(component: DefaultTwentyFortyEightSessionComponent): TwentyFortyEightStore =
            component.retainedStore

        @Provides
        @SingleIn(MiniAppSessionScope::class)
        internal fun provideSession(component: TwentyFortyEightSessionComponent): TwentyFortyEightSession =
            TwentyFortyEightSession(component)
    }
}
