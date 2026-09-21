package ge.yet.game.fallingblocks.component.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import ge.yet.game.fallingblocks.component.game.FallingBlocksComponent
import ge.yet.game.fallingblocks.component.result.ResultComponent
import ge.yet.game.miniapp.compose.MiniAppFrameMode

internal interface RootComponent {
    val stack: Value<ChildStack<*, Child>>
    val frameMode: Value<MiniAppFrameMode>
    fun handleBack(): Boolean

    sealed interface Child {
        class Playing(val component: FallingBlocksComponent) : Child
        class Result(val component: ResultComponent) : Child
    }

    fun interface Factory {
        fun create(componentContext: ComponentContext): RootComponent
    }
}
