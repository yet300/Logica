package ge.yet.game.fallingblocks.ui.screen.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import ge.yet.game.fallingblocks.component.root.RootComponent
import ge.yet.game.fallingblocks.ui.result.FallingBlocksResultContent
import ge.yet.game.fallingblocks.ui.screen.game.FallingBlocksScreen
import ge.yet.game.miniapp.compose.MiniAppAdKind
import ge.yet.game.miniapp.compose.MiniAppAdsCapability

@Composable
internal fun RootContent(
    component: RootComponent,
    ads: MiniAppAdsCapability,
    modifier: Modifier = Modifier,
) {
    Children(
        stack = component.stack,
        modifier = modifier,
        animation = stackAnimation(fade()),
    ) { child ->
        when (val instance = child.instance) {
            is RootComponent.Child.Playing -> FallingBlocksScreen(
                component = instance.component,
                modifier = Modifier.fillMaxSize(),
            )
            is RootComponent.Child.Result -> {
                val gate = ads.rememberGate(
                    MiniAppAdKind.Fullscreen("continue_after_game_over"),
                )
                FallingBlocksResultContent(
                    component = instance.component,
                    interstitialGate = gate,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
