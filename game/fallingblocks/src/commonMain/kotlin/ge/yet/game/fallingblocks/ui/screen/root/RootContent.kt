package ge.yet.game.fallingblocks.ui.screen.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ge.yet.game.fallingblocks.component.root.RootComponent
import ge.yet.game.fallingblocks.ui.screen.game.FallingBlocksScreen

@Composable
internal fun RootContent(component: RootComponent, modifier: Modifier = Modifier) {
    FallingBlocksScreen(component = component.playing, modifier = modifier)
}
