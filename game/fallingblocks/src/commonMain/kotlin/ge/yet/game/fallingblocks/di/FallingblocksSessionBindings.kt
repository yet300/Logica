package ge.yet.game.fallingblocks.di

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import ge.yet.game.fallingblocks.FallingblocksSession
import ge.yet.game.fallingblocks.audio.FallingBlocksAudioAdapter
import ge.yet.game.fallingblocks.audio.FallingBlocksAudioPlayer
import ge.yet.game.fallingblocks.component.game.DefaultFallingBlocksComponentFactory
import ge.yet.game.fallingblocks.component.game.FallingBlocksComponent
import ge.yet.game.fallingblocks.component.game.store.DefaultNewGameSeedSource
import ge.yet.game.fallingblocks.component.game.store.NewGameSeedSource
import ge.yet.game.fallingblocks.component.result.DefaultResultComponentFactory
import ge.yet.game.fallingblocks.component.result.ResultComponent
import ge.yet.game.fallingblocks.component.root.DefaultRootComponentFactory
import ge.yet.game.fallingblocks.component.root.RootComponent
import ge.yet.game.fallingblocks.data.FallingBlocksPersistence
import ge.yet.game.fallingblocks.domain.engine.DefaultFallingBlocksEngine
import ge.yet.game.fallingblocks.domain.model.FallingBlocksEngine
import ge.yet.game.fallingblocks.domain.repository.GameCommitWriter
import ge.yet.game.fallingblocks.domain.repository.GameSnapshotLoader
import ge.yet.game.fallingblocks.domain.repository.TutorialSeenRepository
import ge.yet.game.miniapp.compose.MiniAppAdsCapability
import ge.yet.game.miniapp.metro.MiniAppSessionScope

@BindingContainer
abstract class FallingblocksSessionBindings {
    @Binds
    internal abstract fun bindLoader(impl: FallingBlocksPersistence): GameSnapshotLoader
    @Binds
    internal abstract fun bindWriter(impl: FallingBlocksPersistence): GameCommitWriter
    @Binds
    internal abstract fun bindTutorial(impl: FallingBlocksPersistence): TutorialSeenRepository
    @Binds
    internal abstract fun bindSeed(impl: DefaultNewGameSeedSource): NewGameSeedSource
    @Binds
    internal abstract fun bindAudio(impl: FallingBlocksAudioAdapter): FallingBlocksAudioPlayer
    @Binds
    internal abstract fun bindGameFactory(impl: DefaultFallingBlocksComponentFactory): FallingBlocksComponent.Factory
    @Binds
    internal abstract fun bindResultFactory(impl: DefaultResultComponentFactory): ResultComponent.Factory
    @Binds
    internal abstract fun bindRootFactory(impl: DefaultRootComponentFactory): RootComponent.Factory

    companion object {
        @Provides
        internal fun provideEngine(): FallingBlocksEngine = DefaultFallingBlocksEngine

        @Provides
        @SingleIn(MiniAppSessionScope::class)
        internal fun provideRoot(factory: RootComponent.Factory, context: ComponentContext): RootComponent =
            factory.create(context)

        @Provides
        @SingleIn(MiniAppSessionScope::class)
        internal fun provideSession(
            component: RootComponent,
            ads: MiniAppAdsCapability,
        ): FallingblocksSession = FallingblocksSession(component, ads)
    }
}