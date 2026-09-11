package ge.yet.game.fruitmerge

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import ge.yet.game.fruitmerge.component.game.PaidActionToken
import ge.yet.game.fruitmerge.component.session.FruitMergeSessionComponent
import ge.yet.game.fruitmerge.ui.FruitMergeResultScreen
import ge.yet.game.fruitmerge.ui.FruitMergeScreen

@Composable
internal fun FruitMergeContent(
    component: FruitMergeSessionComponent,
    requestClearAd: (PaidActionToken) -> Unit,
    requestShakeAd: (PaidActionToken) -> Unit,
    modifier: Modifier = Modifier,
) {
    Children(
        stack = component.stack,
        modifier = modifier,
        // Fade only: alpha compositing is GPU-cheap, while scale() forces
        // the full-screen board canvas to re-rasterize every frame and janks.
        animation = stackAnimation(
            fade(
                animationSpec = tween(
                    durationMillis = RESULT_CROSSFADE_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            ),
        ),
    ) { child ->
        when (val instance = child.instance) {
            is FruitMergeSessionComponent.Child.Playing -> FruitMergeScreen(
                component = instance.component,
                requestClearAd = requestClearAd,
                requestShakeAd = requestShakeAd,
                modifier = Modifier,
            )

            is FruitMergeSessionComponent.Child.Result -> FruitMergeResultScreen(
                component = instance.component,
                modifier = Modifier,
            )
        }
    }
}

private const val RESULT_CROSSFADE_MILLIS: Int = 450
