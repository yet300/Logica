package ge.yet.game.fruitmerge

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
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
    Children(stack = component.stack, modifier = modifier) { child ->
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
