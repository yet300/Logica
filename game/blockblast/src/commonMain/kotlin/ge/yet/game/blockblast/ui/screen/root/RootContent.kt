package ge.yet.game.blockblast.ui.screen.root

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ge.yet.game.blockblast.component.root.RootComponent
import ge.yet.game.blockblast.ui.game.BlockBlastGameContent
import ge.yet.game.blockblast.ui.result.GameResultContent
import ge.yet.game.miniapp.compose.MiniAppAdKind
import ge.yet.game.miniapp.compose.MiniAppAdsCapability
import ge.yet.game.uikit.components.background.AmbientMeshBackground

@Composable
internal fun RootContent(
    component: RootComponent,
    interstitials: MiniAppAdsCapability,
    modifier: Modifier = Modifier,
) {
    val stack by component.stack.subscribeAsState()

    Children(
        stack = stack,
        modifier = modifier,
        animation = stackAnimation(fade()),
    ) { child ->
        when (val instance = child.instance) {
            is RootComponent.Child.Playing -> BlockBlastGameContent(
                component = instance.component,
                modifier = Modifier,
            )

            is RootComponent.Child.Result -> {
                val gate = interstitials.rememberGate(
                    MiniAppAdKind.Fullscreen("continue_after_game_over"),
                )
                GameResultContent(
                    component = instance.component,
                    interstitialGate = gate,
                    modifier = Modifier,
                )
            }
        }
    }
}

@Composable
internal fun RootBackground(
    modifier: Modifier = Modifier,
) {
    AmbientMeshBackground(
        modifier = modifier.testTag("blockblast_ambient_background"),
        baseColor = MaterialTheme.colorScheme.background,
    )
}
