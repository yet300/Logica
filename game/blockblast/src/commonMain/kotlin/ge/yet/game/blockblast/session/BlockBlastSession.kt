package ge.yet.game.blockblast.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ge.yet.game.blockblast.component.root.RootComponent
import ge.yet.game.blockblast.ui.screen.root.RootBackground
import ge.yet.game.blockblast.ui.screen.root.RootContent
import ge.yet.game.blockblast.ui.screen.root.RootTopBarContent
import ge.yet.game.blockblast.ui.LocalSoundEnabled
import ge.yet.game.blockblast.ui.LocalVibrationEnabled
import ge.yet.game.domain.repository.FeedbackPreferences
import ge.yet.game.miniapp.compose.DelegatingMiniAppSession
import ge.yet.game.miniapp.compose.MiniAppAdsCapability

class BlockBlastSession internal constructor(
    internal val component: RootComponent,
    private val interstitials: MiniAppAdsCapability,
    internal val feedback: FeedbackPreferences,
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
    override fun Background(modifier: Modifier) {
        RootBackground(modifier)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val vibrationEnabled by feedback.vibrationEnabled.collectAsState()
        val soundEnabled by feedback.sfxEnabled.collectAsState()

        CompositionLocalProvider(
            LocalVibrationEnabled provides vibrationEnabled,
            LocalSoundEnabled provides soundEnabled,
        ) {
            RootContent(
                component = component,
                interstitials = interstitials,
                modifier = modifier,
            )
        }
    }
}
