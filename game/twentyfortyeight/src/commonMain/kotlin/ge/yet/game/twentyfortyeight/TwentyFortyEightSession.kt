package ge.yet.game.twentyfortyeight

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ge.yet.game.miniapp.compose.DelegatingMiniAppSession
import ge.yet.game.twentyfortyeight.component.root.RootComponent
import ge.yet.game.twentyfortyeight.ui.screen.root.RootContent

class TwentyFortyEightSession internal constructor(
    internal val component: RootComponent,
) : DelegatingMiniAppSession(
    frameMode = component.frameMode,
    wantsBanner = true,
    onBack = component::handleBack,
) {
    @Composable
    override fun Content(modifier: Modifier) {
        RootContent(component = component, modifier = modifier)
    }
}
