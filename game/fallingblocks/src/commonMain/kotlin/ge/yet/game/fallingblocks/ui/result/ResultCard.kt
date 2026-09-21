package ge.yet.game.fallingblocks.ui.result

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.common.utils.formatScore
import ge.yet.game.fallingblocks.component.result.ResultComponent
import ge.yet.game.uikit.components.modifier.ringShadow
import ge.yet.game.uikit.components.modifier.whisperShadow

@Composable
internal fun ResultCard(
    model: ResultComponent.Model,
    scoreLabel: String,
    bestLabel: String,
    continueLabel: String,
    newGameLabel: String,
    advertisementLabel: String?,
    layoutPolicy: ResultLayoutPolicy,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(
        when {
            layoutPolicy.isUltraCompact -> 20.dp
            layoutPolicy.isCompact -> 24.dp
            else -> 28.dp
        },
    )
    val primaryText = if (model.isContinuePhase) {
        "$continueLabel (${model.continueSecondsRemaining})"
    } else {
        newGameLabel
    }
    val actionDescription = if (model.isContinuePhase && advertisementLabel != null) {
        "$primaryText. $advertisementLabel"
    } else {
        primaryText
    }

    Column(
        modifier = modifier
            .whisperShadow(shape = shape, elevation = 24.dp)
            .ringShadow(color = MaterialTheme.colorScheme.outline, shape = shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .padding(
                horizontal = layoutPolicy.cardHorizontalPaddingDp.dp,
                vertical = layoutPolicy.cardVerticalPaddingDp.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = scoreLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = model.score.formatScore(),
            style = when {
                layoutPolicy.isUltraCompact -> MaterialTheme.typography.headlineMedium
                layoutPolicy.isCompact -> MaterialTheme.typography.headlineLarge
                else -> MaterialTheme.typography.displayMedium
            },
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(layoutPolicy.scoreSpacingDp.dp))
        Text(
            text = "$bestLabel · ${model.bestScore.formatScore()}",
            style = if (layoutPolicy.isUltraCompact) {
                MaterialTheme.typography.bodySmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(layoutPolicy.sectionSpacingDp.dp))
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .height(layoutPolicy.buttonHeightDp.dp)
                .testTag(FallingBlocksResultTags.Primary)
                .semantics { contentDescription = actionDescription },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (model.isContinuePhase && advertisementLabel != null) {
                    AdPlayIcon(Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(primaryText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun AdPlayIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier) {
        val stroke = size.minDimension * 0.09f
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(size.minDimension * 0.18f),
            style = Stroke(stroke),
        )
        val path = Path().apply {
            moveTo(size.width * 0.42f, size.height * 0.30f)
            lineTo(size.width * 0.72f, size.height * 0.50f)
            lineTo(size.width * 0.42f, size.height * 0.70f)
            close()
        }
        drawPath(path, color)
    }
}
