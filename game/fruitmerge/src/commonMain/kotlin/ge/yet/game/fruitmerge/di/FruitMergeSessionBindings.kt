package ge.yet.game.fruitmerge.di

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import ge.yet.game.fruitmerge.data.FruitMergePersistence
import ge.yet.game.fruitmerge.domain.engine.FruitMergeEngine
import ge.yet.game.fruitmerge.domain.engine.FruitMergeRules
import ge.yet.game.fruitmerge.domain.repository.GameCommitWriter
import ge.yet.game.fruitmerge.domain.repository.GameSnapshotLoader
import ge.yet.game.fruitmerge.domain.repository.TutorialSeenRepository
import ge.yet.game.fruitmerge.FruitMergeSession
import ge.yet.game.fruitmerge.component.game.DefaultFruitMergeComponentFactory
import ge.yet.game.fruitmerge.component.game.FruitMergeComponent
import ge.yet.game.fruitmerge.component.result.DefaultFruitMergeResultComponentFactory
import ge.yet.game.fruitmerge.component.result.FruitMergeResultComponent
import ge.yet.game.fruitmerge.component.root.DefaultRootComponent
import ge.yet.game.fruitmerge.component.root.DefaultRootComponentFactory
import ge.yet.game.fruitmerge.component.root.RootComponent
import ge.yet.game.miniapp.compose.MiniAppInterstitialCapability
import ge.yet.game.miniapp.metro.MiniAppSessionScope

@BindingContainer
abstract class FruitMergeSessionBindings {
    @Binds
    internal abstract fun bindRules(impl: FruitMergeEngine): FruitMergeRules

    @Binds
    internal abstract fun bindSnapshotLoader(persistence: FruitMergePersistence): GameSnapshotLoader

    @Binds
    internal abstract fun bindCommitWriter(persistence: FruitMergePersistence): GameCommitWriter

    @Binds
    internal abstract fun bindTutorialSeen(persistence: FruitMergePersistence): TutorialSeenRepository

    @Binds
    internal abstract fun bindGameComponentFactory(
        impl: DefaultFruitMergeComponentFactory,
    ): FruitMergeComponent.Factory

    @Binds
    internal abstract fun bindResultComponentFactory(
        impl: DefaultFruitMergeResultComponentFactory,
    ): FruitMergeResultComponent.Factory

    @Binds
    internal abstract fun bindSessionComponentFactory(
        impl: DefaultRootComponentFactory,
    ): RootComponent.Factory

    @Binds
    internal abstract fun bindComponentContract(
        component: DefaultRootComponent,
    ): RootComponent

    companion object {
        @Provides
        @SingleIn(MiniAppSessionScope::class)
        internal fun provideComponent(
            factory: RootComponent.Factory,
            componentContext: ComponentContext,
        ): DefaultRootComponent =
            // Safe: the only Factory binding in this scope is DefaultRootComponentFactory,
            // whose create() returns the Default type (covariant override). Concrete type is kept
            // because provideSession exposes the graph-retained session.
            factory.create(componentContext) as DefaultRootComponent

        @Provides
        @SingleIn(MiniAppSessionScope::class)
        internal fun provideSession(
            component: RootComponent,
            interstitials: MiniAppInterstitialCapability,
        ): FruitMergeSession = FruitMergeSession(component, interstitials)
    }
}
