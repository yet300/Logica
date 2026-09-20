package ge.yet.game.fallingblocks.component.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.Value
import ge.yet.game.fallingblocks.component.game.FallingBlocksComponent
import ge.yet.game.fallingblocks.component.result.ResultComponent
import ge.yet.game.miniapp.compose.MiniAppFrameMode

internal interface RootComponent {
    val playing: FallingBlocksComponent
    val result: Value<ChildSlot<*, ResultComponent>>
    val frameMode: Value<MiniAppFrameMode>
    fun handleBack(): Boolean

    fun interface Factory {
        fun create(componentContext: ComponentContext): RootComponent
    }
}
