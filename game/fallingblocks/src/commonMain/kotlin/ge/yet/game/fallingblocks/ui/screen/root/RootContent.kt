package ge.yet.game.fallingblocks.ui.screen.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ge.yet.game.fallingblocks.component.root.RootComponent
import ge.yet.game.fallingblocks.ui.result.ResultOverlay
import ge.yet.game.fallingblocks.ui.screen.game.FallingBlocksScreen
import ge.yet.game.miniapp.compose.MiniAppAdKind
import ge.yet.game.miniapp.compose.MiniAppAdsCapability

@Composable
internal fun RootContent(
    component: RootComponent,
    ads: MiniAppAdsCapability,
    modifier: Modifier = Modifier,
) {
    val result by component.result.subscribeAsState()
    Box(modifier = modifier.fillMaxSize()) {
        FallingBlocksScreen(
            component = component.playing,
            modifier = Modifier.fillMaxSize(),
        )
        result.child?.instance?.let { resultComponent ->
            val gate = ads.rememberGate(
                MiniAppAdKind.Fullscreen("continue_after_game_over"),
            )
            ResultOverlay(
                component = resultComponent,
                advertisementExpected = gate.willShowAd,
                onPrimary = { resultComponent.onPrimaryClicked(gate.request) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
