package ge.yet.game.fruitmerge.component.session

import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import dev.zacsweers.metro.Inject
import ge.yet.game.fruitmerge.audio.FruitMergeAudioAdapter
import ge.yet.game.fruitmerge.component.game.DefaultFruitMergeComponent
import ge.yet.game.fruitmerge.component.game.FruitMergeComponent
import ge.yet.game.fruitmerge.component.game.PaidActionToken
import ge.yet.game.fruitmerge.component.game.store.FruitMergeStore
import ge.yet.game.fruitmerge.component.game.store.FruitMergeStoreFactory
import ge.yet.game.miniapp.compose.MiniAppFrameMode
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

internal class DefaultFruitMergeSessionComponent(
    componentContext: ComponentContext,
    private val gameFactory: FruitMergeComponent.Factory,
    storeFactory: FruitMergeStoreFactory,
    private val audio: FruitMergeAudioAdapter,
) : FruitMergeSessionComponent,
    ComponentContext by componentContext {
    internal val retainedStore: FruitMergeStore = instanceKeeper.getStore(storeFactory::create)
    internal val gameComponent = gameFactory.create(
        componentContext = childContext(key = "FruitMergeGame"),
        store = retainedStore,
    ) as DefaultFruitMergeComponent
    override val game: FruitMergeComponent = gameComponent
    override val frameMode: Value<MiniAppFrameMode> = game.model.map { model ->
        when (model.screen) {
            is FruitMergeComponent.ScreenState.Playing -> MiniAppFrameMode.Standard
            is FruitMergeComponent.ScreenState.GameOver -> MiniAppFrameMode.ContentOnly
        }
    }

    init {
        audio.start()
        val scope = coroutineScope()
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            retainedStore.labels.collect { label ->
                gameComponent.onStoreLabel(label)
                audio.play(label)
            }
        }
    }

    override fun completePaidAction(token: PaidActionToken) {
        game.completePaidAction(token)
    }

    override fun handleBack(): Boolean = game.handleBack()
}

@Inject
internal class DefaultFruitMergeSessionComponentFactory(
    private val gameFactory: FruitMergeComponent.Factory,
    private val storeFactory: FruitMergeStoreFactory,
    private val audio: FruitMergeAudioAdapter,
) : FruitMergeSessionComponent.Factory {
    override fun create(
        componentContext: ComponentContext,
    ): DefaultFruitMergeSessionComponent = DefaultFruitMergeSessionComponent(
        componentContext = componentContext,
        gameFactory = gameFactory,
        storeFactory = storeFactory,
        audio = audio,
    )
}
