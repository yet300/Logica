package ge.yet.game.twentyfortyeight

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.value.Value
import ge.yet.game.miniapp.compose.MiniAppFrameMode
import ge.yet.game.miniapp.compose.MiniAppSession
import ge.yet.game.twentyfortyeight.component.root.RootComponent
import ge.yet.game.twentyfortyeight.ui.screen.root.RootContent

class TwentyFortyEightSession internal constructor(
    internal val component: RootComponent,
) : MiniAppSession {
    override val frameMode: Value<MiniAppFrameMode> = component.frameMode

    override val wantsBanner: Boolean = true

    override fun handleBack(): Boolean = component.handleBack()

    @Composable
    override fun Content(modifier: Modifier) {
        RootContent(component = component, modifier = modifier)
    }
}
