package ge.yet.game.fallingblocks

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.value.MutableValue
import ge.yet.game.fallingblocks.component.root.RootComponent
import ge.yet.game.fallingblocks.ui.screen.root.RootContent
import ge.yet.game.miniapp.compose.DelegatingMiniAppSession
import ge.yet.game.miniapp.compose.MiniAppFrameMode

class FallingblocksSession internal constructor(
    private val component: RootComponent,
) : DelegatingMiniAppSession(
    frameMode = MutableValue(MiniAppFrameMode.Standard),
    wantsBanner = true,
) {
    @Composable
    override fun Content(modifier: Modifier) {
        RootContent(component = component, modifier = modifier)
    }
}
