package ge.yet.game.fallingblocks.ui.screen.root

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ge.yet.game.fallingblocks.component.root.RootComponent
import ge.yet.game.fallingblocks.generated.resources.Res
import ge.yet.game.fallingblocks.generated.resources.best_label
import ge.yet.game.fallingblocks.generated.resources.score_label
import ge.yet.game.uikit.components.score.CompactScoreCard
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RootTopBarContent(component: RootComponent) {
    val stack by component.stack.subscribeAsState()
    val playing = (stack.active.instance as? RootComponent.Child.Playing)?.component ?: return
    val model by playing.model.subscribeAsState()
    if (model.loading) return
    FallingBlocksScoreHeader(
        score = model.game?.score ?: 0,
        bestScore = model.bestScore,
    )
}

@Composable
internal fun FallingBlocksScoreHeader(score: Long, bestScore: Long) {
    CompactScoreCard(
        primaryLabel = stringResource(Res.string.score_label),
        primaryValue = score,
        secondaryLabel = stringResource(Res.string.best_label),
        secondaryValue = bestScore,
        modifier = Modifier
            .widthIn(min = 180.dp, max = 260.dp)
            .testTag("falling_blocks_score_header"),
    )
}
