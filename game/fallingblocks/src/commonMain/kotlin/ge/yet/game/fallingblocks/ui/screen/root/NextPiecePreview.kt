package ge.yet.game.fallingblocks.ui.screen.root

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ge.yet.game.fallingblocks.domain.engine.cells
import ge.yet.game.fallingblocks.domain.model.ActivePiece
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.Rotation
import ge.yet.game.fallingblocks.domain.model.Tetromino
import ge.yet.game.fallingblocks.generated.resources.Res
import ge.yet.game.fallingblocks.generated.resources.next_label
import ge.yet.game.fallingblocks.ui.board.colors
import ge.yet.game.fallingblocks.ui.screen.game.FallingBlocksTestTags
import ge.yet.game.uikit.components.modifier.ringShadow
import ge.yet.game.uikit.components.modifier.whisperShadow
import org.jetbrains.compose.resources.stringResource

/**
 * "Next" piece pill for the host app bar.
 *
 * Container matches the Block Blast style score pills (18dp, surface,
 * whisper + ring) so the whole top bar reads as one family; the piece itself
 * keeps the game look from [ge.yet.game.fallingblocks.ui.board.CrtBoard]:
 * fill + top highlight + outline with board proportions.
 */
@Composable
internal fun NextPiecePreview(
    piece: Tetromino?,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val pillSize = appBarPillSize(compact)
    Column(
        modifier = modifier
            .size(pillSize.width, pillSize.height)
            .whisperShadow(shape = shape)
            .ringShadow(color = scheme.outline, shape = shape)
            .background(scheme.surface, shape)
            .padding(
                horizontal = if (compact) 10.dp else 16.dp,
                vertical = if (compact) 6.dp else 8.dp,
            )
            .testTag(FallingBlocksTestTags.Preview),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.next_label),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
        Canvas(
            modifier = Modifier
                .size(
                    width = if (compact) 52.dp else 64.dp,
                    height = if (compact) 28.dp else 32.dp,
                )
                .testTag(FallingBlocksTestTags.PreviewPiece),
        ) {
            if (piece == null) return@Canvas
            val cells = ActivePiece(piece, Rotation.SPAWN, Cell(0, 0)).cells()
            val minX = cells.minOf { it.x }
            val maxX = cells.maxOf { it.x }
            val minY = cells.minOf { it.y }
            val maxY = cells.maxOf { it.y }
            val cell = minOf(
                size.width / (maxX - minX + 1),
                size.height / (maxY - minY + 1),
            ).coerceAtLeast(1f)
            val pieceWidth = (maxX - minX + 1) * cell
            val pieceHeight = (maxY - minY + 1) * cell
            val origin = Offset(
                (size.width - pieceWidth) / 2f,
                (size.height - pieceHeight) / 2f,
            )
            val colors = piece.colors(scheme)
            cells.forEach { block ->
                val base = origin + Offset(
                    (block.x - minX) * cell,
                    (block.y - minY) * cell,
                )
                val gap = cell * 0.075f
                val topLeft = base + Offset(gap, gap)
                val tileSize = Size(cell - gap * 2f, cell - gap * 2f)
                val radius = CornerRadius(cell * 0.14f)
                drawRoundRect(
                    color = colors.fill,
                    topLeft = topLeft,
                    size = tileSize,
                    cornerRadius = radius,
                )
                drawRoundRect(
                    color = colors.highlight.copy(alpha = 0.64f),
                    topLeft = topLeft + Offset(gap, gap),
                    size = Size(tileSize.width - gap * 2f, cell * 0.10f),
                    cornerRadius = radius,
                )
                drawRoundRect(
                    color = colors.outline.copy(alpha = 0.72f),
                    topLeft = topLeft,
                    size = tileSize,
                    cornerRadius = radius,
                    style = Stroke(maxOf(1f, cell * 0.045f)),
                )
            }
        }
    }
}
