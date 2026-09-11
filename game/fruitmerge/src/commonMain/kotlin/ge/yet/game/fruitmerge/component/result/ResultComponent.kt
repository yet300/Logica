package ge.yet.game.fruitmerge.component.result

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value

internal interface FruitMergeResultComponent {
    val model: Value<Model>

    fun onNewGame()

    data class Model(
        val snapshot: FruitMergeResultSnapshot,
    )

    fun interface Factory {
        fun create(
            componentContext: ComponentContext,
            snapshot: FruitMergeResultSnapshot,
            onNewGameRequested: () -> Unit,
        ): FruitMergeResultComponent
    }
}
