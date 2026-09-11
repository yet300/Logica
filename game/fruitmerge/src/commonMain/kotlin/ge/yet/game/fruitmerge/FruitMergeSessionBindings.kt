package ge.yet.game.fruitmerge

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import ge.yet.game.fruitmerge.audio.FruitMergeAudioAdapter
import ge.yet.game.fruitmerge.engine.FruitMergeEngine
import ge.yet.game.fruitmerge.engine.FruitMergeRules
import ge.yet.game.fruitmerge.persistence.FruitMergePersistence
import ge.yet.game.fruitmerge.session.DefaultFruitMergeComponentFactory
import ge.yet.game.fruitmerge.session.DefaultFruitMergeSessionComponent
import ge.yet.game.fruitmerge.session.DefaultFruitMergeSessionComponentFactory
import ge.yet.game.fruitmerge.session.FruitMergeComponent
import ge.yet.game.fruitmerge.session.FruitMergeSessionComponent
import ge.yet.game.fruitmerge.store.FruitMergeStore
import ge.yet.game.fruitmerge.store.FruitMergeStoreFactory
import ge.yet.game.miniapp.api.MiniAppStorage
import ge.yet.game.miniapp.api.MiniAppVisibilitySource
import ge.yet.game.miniapp.audio.MiniAppAudio
import ge.yet.game.miniapp.compose.MiniAppInterstitialCapability
import ge.yet.game.miniapp.metro.MiniAppSessionScope

@BindingContainer
abstract class FruitMergeSessionBindings {
    @Binds
    internal abstract fun bindGameComponentFactory(
        impl: DefaultFruitMergeComponentFactory,
    ): FruitMergeComponent.Factory

    @Binds
    internal abstract fun bindSessionComponentFactory(
        impl: DefaultFruitMergeSessionComponentFactory,
    ): FruitMergeSessionComponent.Factory

    @Binds
    internal abstract fun bindComponentContract(
        component: DefaultFruitMergeSessionComponent,
    ): FruitMergeSessionComponent

    companion object {
        @Provides
        @SingleIn(MiniAppSessionScope::class)
        internal fun provideRules(): FruitMergeRules = FruitMergeEngine()

        @Provides
        @SingleIn(MiniAppSessionScope::class)
        internal fun providePersistence(storage: MiniAppStorage): FruitMergePersistence =
            FruitMergePersistence(storage)

        @Provides
        @SingleIn(MiniAppSessionScope::class)
        internal fun provideAudioAdapter(audio: MiniAppAudio): FruitMergeAudioAdapter =
            FruitMergeAudioAdapter(audio)

        @Provides
        @SingleIn(MiniAppSessionScope::class)
        internal fun provideComponent(
            factory: FruitMergeSessionComponent.Factory,
            componentContext: ComponentContext,
        ): DefaultFruitMergeSessionComponent =
            // Safe: the only Factory binding in this scope is DefaultFruitMergeSessionComponentFactory,
            // whose create() returns the Default type (covariant override). Concrete type is kept
            // because provideStore exposes the retained store for graph inspection.
            factory.create(componentContext) as DefaultFruitMergeSessionComponent

        @Provides
        @SingleIn(MiniAppSessionScope::class)
        internal fun provideStore(component: DefaultFruitMergeSessionComponent): FruitMergeStore =
            component.retainedStore

        @Provides
        @SingleIn(MiniAppSessionScope::class)
        internal fun provideSession(
            component: FruitMergeSessionComponent,
            interstitials: MiniAppInterstitialCapability,
        ): FruitMergeSession = FruitMergeSession(component, interstitials)
    }
}
