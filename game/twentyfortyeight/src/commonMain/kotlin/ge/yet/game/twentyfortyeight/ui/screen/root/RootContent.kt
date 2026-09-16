package ge.yet.game.twentyfortyeight.ui.screen.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ge.yet.game.twentyfortyeight.component.root.RootComponent
import ge.yet.game.twentyfortyeight.ui.screen.TwentyFortyEightScreen

@Composable
internal fun RootContent(
    component: RootComponent,
    modifier: Modifier = Modifier,
) = TwentyFortyEightScreen(component = component, modifier = modifier)
