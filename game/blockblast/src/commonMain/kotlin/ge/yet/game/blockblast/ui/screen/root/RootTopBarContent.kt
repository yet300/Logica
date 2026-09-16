package ge.yet.game.blockblast.ui.screen.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ge.yet.game.blockblast.generated.resources.Res
import ge.yet.game.blockblast.generated.resources.best
import ge.yet.game.blockblast.generated.resources.score
import ge.yet.game.blockblast.component.root.RootComponent
import ge.yet.game.blockblast.ui.game.ScoreHeader
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RootTopBarContent(component: RootComponent) {
    val stack by component.stack.subscribeAsState()
    val playing = stack.active.instance as? RootComponent.Child.Playing ?: return
    val model by playing.component.model.subscribeAsState()

    ScoreHeader(
        score = model.game.score,
        bestScore = model.game.bestScore,
        scoreLabel = stringResource(Res.string.score),
        bestLabel = stringResource(Res.string.best),
        modifier = Modifier.testTag("blockblast_score_header"),
    )
}
