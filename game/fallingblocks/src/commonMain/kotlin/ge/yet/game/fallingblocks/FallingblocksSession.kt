package ge.yet.game.fallingblocks

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ge.yet.game.fallingblocks.component.root.RootComponent
import ge.yet.game.fallingblocks.ui.screen.root.RootContent
import ge.yet.game.fallingblocks.ui.screen.root.RootTopBarContent
import ge.yet.game.miniapp.compose.DelegatingMiniAppSession
import ge.yet.game.miniapp.compose.MiniAppAdsCapability

class FallingblocksSession internal constructor(
    private val component: RootComponent,
    private val ads: MiniAppAdsCapability,
) : DelegatingMiniAppSession(
    frameMode = component.frameMode,
    wantsBanner = true,
    onBack = component::handleBack,
) {
    @Composable
    override fun TopBarContent() {
        RootTopBarContent(component)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        RootContent(component = component, ads = ads, modifier = modifier)
    }
}
