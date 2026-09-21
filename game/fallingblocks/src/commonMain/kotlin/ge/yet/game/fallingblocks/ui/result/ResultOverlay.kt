package ge.yet.game.fallingblocks.ui.result

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ge.yet.game.fallingblocks.component.result.ResultComponent
import ge.yet.game.fallingblocks.generated.resources.Res
import ge.yet.game.fallingblocks.generated.resources.advertisement_suffix
import ge.yet.game.fallingblocks.generated.resources.best_label
import ge.yet.game.fallingblocks.generated.resources.game_over_title
import ge.yet.game.fallingblocks.generated.resources.new_game_action
import ge.yet.game.fallingblocks.generated.resources.score_label
import ge.yet.game.fallingblocks.generated.resources.try_again_action
import org.jetbrains.compose.resources.stringResource

internal const val RESULT_SCRIM_ALPHA = 0f
internal const val RESULT_USES_BLUR = false

internal object ResultOverlayTags {
    const val Root = "falling_blocks_result_overlay"
    const val Panel = "falling_blocks_result_panel"
    const val Primary = "falling_blocks_result_primary"
}

@Composable
internal fun ResultOverlay(
    component: ResultComponent,
    advertisementExpected: Boolean,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val model by component.model.subscribeAsState()
    ResultOverlay(model, advertisementExpected, onPrimary, modifier)
}

@Composable
internal fun ResultOverlay(
    model: ResultComponent.Model,
    advertisementExpected: Boolean,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val entrance by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "result panel entrance",
    )
    val isContinue = model.isContinuePhase
    val action = stringResource(
        if (isContinue) Res.string.try_again_action else Res.string.new_game_action,
    )
    val advertisement = stringResource(Res.string.advertisement_suffix)
    val actionDescription = if (isContinue && advertisementExpected) {
        "$action. $advertisement"
    } else {
        action
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(ResultOverlayTags.Root),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .graphicsLayer {
                    alpha = entrance
                    translationY = (1f - entrance) * 48.dp.toPx()
                }
                .testTag(ResultOverlayTags.Panel),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(Res.string.game_over_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ResultMetric(stringResource(Res.string.score_label), model.score)
                    ResultMetric(stringResource(Res.string.best_label), model.bestScore)
                }
                if (isContinue) {
                    Text(
                        text = model.continueSecondsRemaining.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Button(
                    onClick = onPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag(ResultOverlayTags.Primary)
                        .semantics { contentDescription = actionDescription },
                ) {
                    Text(action)
                }
            }
        }
    }
}

@Composable
private fun ResultMetric(label: String, value: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
