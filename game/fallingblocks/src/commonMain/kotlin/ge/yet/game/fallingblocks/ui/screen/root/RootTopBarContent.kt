package ge.yet.game.fallingblocks.ui.screen.root

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RootTopBarContent(component: RootComponent) {
    val model by component.playing.model.subscribeAsState()
    if (model.loading) return
    Row(
        modifier = Modifier.testTag("falling_blocks_score_header"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToolbarMetric(stringResource(Res.string.score_label), model.game?.score ?: 0)
        ToolbarMetric(stringResource(Res.string.best_label), model.bestScore)
    }
}

@Composable
private fun ToolbarMetric(label: String, value: Long) {
    Text(
        text = "$label $value",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
