package ge.yet.game.fallingblocks.ui.screen.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ge.yet.game.fallingblocks.component.root.RootComponent
import ge.yet.game.fallingblocks.generated.resources.Res
import ge.yet.game.fallingblocks.generated.resources.best_label
import ge.yet.game.fallingblocks.generated.resources.score_label
import ge.yet.game.uikit.components.modifier.ringShadow
import ge.yet.game.uikit.components.modifier.whisperShadow
import ge.yet.game.uikit.components.score.compactScore
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RootTopBarContent(component: RootComponent) {
    val stack by component.stack.subscribeAsState()
    val playing = (stack.active.instance as? RootComponent.Child.Playing)?.component ?: return
    val model by playing.model.subscribeAsState()
    if (model.loading) return
    val next = if (model.tutorialProgress != null) {
        null
    } else {
        model.game?.preview?.firstOrNull()
    }
    // Adapt to the host title slot, not the screen: phones get the compact
    // pills, wide (tablet) slots get roomier padding and a larger preview.
    BoxWithConstraints {
        val compact = maxWidth < 400.dp
        Row(
            horizontalArrangement = Arrangement.spacedBy(
                if (compact) 8.dp else 12.dp,
                Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FallingBlocksScoreHeader(
                score = model.game?.score ?: 0,
                bestScore = model.bestScore,
                compact = compact,
            )
            NextPiecePreview(
                piece = next,
                compact = compact,
            )
        }
    }
}

@Composable
internal fun FallingBlocksScoreHeader(
    score: Long,
    bestScore: Long,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
) {
    // bestScore tracks the live max (see FallingBlocksStoreFactory), so once the
    // record is beaten score == bestScore: collapse to a single highlighted pill
    // instead of showing the same number twice.
    val isRecord = score > 0 && score >= bestScore
    Row(
        modifier = modifier.testTag("falling_blocks_score_header"),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isRecord) {
            ScorePill(
                label = stringResource(Res.string.best_label),
                value = score,
                highlight = true,
                compact = compact,
            )
        } else {
            ScorePill(
                label = stringResource(Res.string.score_label),
                value = score,
                highlight = false,
                compact = compact,
            )
            ScorePill(
                label = stringResource(Res.string.best_label),
                value = bestScore,
                highlight = true,
                compact = compact,
            )
        }
    }
}

/**
 * Uniform app bar pill size so score and Next pills are symmetric.
 * Compact fits three pills into a phone title slot (3 * 76 + 2 * 8 = 244dp).
 */
internal fun appBarPillSize(compact: Boolean): DpSize =
    if (compact) DpSize(76.dp, 56.dp) else DpSize(100.dp, 64.dp)

internal const val ScorePillTag = "falling_blocks_score_pill"

/**
 * Block Blast style pill: small caption label over the value.
 * Same container language as `ScoreChip` (18dp, surface, whisper + ring),
 * with ellipsis guards so long numbers clip gracefully instead of pushing
 * siblings out of the host title slot.
 */
@Composable
private fun ScorePill(
    label: String,
    value: Long,
    highlight: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val valueColor = if (highlight) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val pillSize = appBarPillSize(compact)
    Column(
        modifier = modifier
            .size(pillSize.width, pillSize.height)
            .testTag(ScorePillTag)
            .whisperShadow(shape = shape)
            .ringShadow(
                color = MaterialTheme.colorScheme.outline,
                shape = shape,
            )
            .background(MaterialTheme.colorScheme.surface, shape)
            .padding(
                horizontal = if (compact) 10.dp else 16.dp,
                vertical = if (compact) 6.dp else 8.dp,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "$label $value"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = compactScore(value),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            ),
            color = valueColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
