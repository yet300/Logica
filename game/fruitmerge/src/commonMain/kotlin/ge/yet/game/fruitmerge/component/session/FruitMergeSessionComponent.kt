package ge.yet.game.fruitmerge.component.session

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import ge.yet.game.fruitmerge.component.game.FruitMergeComponent
import ge.yet.game.fruitmerge.component.game.PaidActionToken
import ge.yet.game.miniapp.compose.MiniAppFrameMode

internal interface FruitMergeSessionComponent {
    val game: FruitMergeComponent
    val frameMode: Value<MiniAppFrameMode>

    fun completePaidAction(token: PaidActionToken)
    fun handleBack(): Boolean

    fun interface Factory {
        fun create(
            componentContext: ComponentContext,
        ): FruitMergeSessionComponent
    }
}
