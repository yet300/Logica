package ge.yet.game.fallingblocks.ui.board

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import ge.yet.game.fallingblocks.domain.model.Tetromino

internal data class GhostStyle(
    val fill: Color,
    val outline: Color,
    val strokeWidthFraction: Float,
)

internal fun ghostStyle(type: Tetromino, scheme: ColorScheme): GhostStyle {
    val board = scheme.surfaceContainerLowest
    val piece = type.colors(scheme)
    val outlineBase = listOf(piece.outline, scheme.onSurface).maxBy { candidate ->
        contrastRatio(candidate.compositeOver(board), board)
    }
    val outline = GHOST_OUTLINE_ALPHAS
        .asSequence()
        .map { alpha -> outlineBase.copy(alpha = alpha) }
        .firstOrNull { candidate ->
            contrastRatio(candidate.compositeOver(board), board) >= MIN_GHOST_CONTRAST
        }
        ?: outlineBase.copy(alpha = 1f)

    return GhostStyle(
        fill = piece.fill.copy(alpha = GHOST_FILL_ALPHA),
        outline = outline,
        strokeWidthFraction = GHOST_STROKE_WIDTH_FRACTION,
    )
}

internal fun contrastRatio(first: Color, second: Color): Double {
    val firstLuminance = first.luminance().toDouble()
    val secondLuminance = second.luminance().toDouble()
    val lighter = maxOf(firstLuminance, secondLuminance)
    val darker = minOf(firstLuminance, secondLuminance)
    return (lighter + 0.05) / (darker + 0.05)
}

private val GHOST_OUTLINE_ALPHAS = listOf(0.72f, 0.84f, 1f)
private const val GHOST_FILL_ALPHA = 0.22f
private const val GHOST_STROKE_WIDTH_FRACTION = 0.075f
private const val MIN_GHOST_CONTRAST = 3.0
