package ge.yet.game.fruitmerge.component.session

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import ge.yet.game.fruitmerge.component.game.FruitMergeComponent
import ge.yet.game.fruitmerge.component.game.PaidActionToken
import ge.yet.game.fruitmerge.component.result.FruitMergeResultComponent
import ge.yet.game.miniapp.compose.MiniAppFrameMode

internal interface FruitMergeSessionComponent {
    val stack: Value<ChildStack<*, Child>>
    val frameMode: Value<MiniAppFrameMode>
    val game: FruitMergeComponent

    fun completePaidAction(token: PaidActionToken)
    fun handleBack(): Boolean

    sealed interface Child {
        class Playing(val component: FruitMergeComponent) : Child
        class Result(val component: FruitMergeResultComponent) : Child
    }

    fun interface Factory {
        fun create(
            componentContext: ComponentContext,
        ): FruitMergeSessionComponent
    }
}
