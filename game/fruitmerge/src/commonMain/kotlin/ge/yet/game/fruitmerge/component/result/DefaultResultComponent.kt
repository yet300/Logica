package ge.yet.game.fruitmerge.component.result

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import dev.zacsweers.metro.Inject

internal class DefaultFruitMergeResultComponent(
    componentContext: ComponentContext,
    snapshot: FruitMergeResultSnapshot,
    private val onNewGameRequested: () -> Unit,
) : FruitMergeResultComponent,
    ComponentContext by componentContext {
    override val model: Value<FruitMergeResultComponent.Model> =
        MutableValue(FruitMergeResultComponent.Model(snapshot))

    override fun onNewGame() = onNewGameRequested()
}

@Inject
internal class DefaultFruitMergeResultComponentFactory : FruitMergeResultComponent.Factory {
    override fun create(
        componentContext: ComponentContext,
        snapshot: FruitMergeResultSnapshot,
        onNewGameRequested: () -> Unit,
    ): DefaultFruitMergeResultComponent = DefaultFruitMergeResultComponent(
        componentContext = componentContext,
        snapshot = snapshot,
        onNewGameRequested = onNewGameRequested,
    )
}
