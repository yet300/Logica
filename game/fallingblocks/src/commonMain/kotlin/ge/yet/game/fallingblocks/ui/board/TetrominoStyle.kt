package ge.yet.game.fallingblocks.ui.board

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import ge.yet.game.fallingblocks.domain.model.Tetromino

internal data class PieceColors(
    val fill: Color,
    val content: Color,
    val highlight: Color,
    val outline: Color,
)

internal fun Tetromino.colors(scheme: ColorScheme): PieceColors {
    val (fill, content) = when (this) {
        Tetromino.I -> scheme.primary to scheme.onPrimary
        Tetromino.J -> scheme.secondary to scheme.onSecondary
        Tetromino.L -> scheme.tertiary to scheme.onTertiary
        Tetromino.O -> scheme.primaryContainer to scheme.onPrimaryContainer
        Tetromino.S -> scheme.secondaryContainer to scheme.onSecondaryContainer
        Tetromino.T -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        Tetromino.Z -> scheme.inversePrimary to scheme.inverseSurface
    }
    return PieceColors(
        fill = fill,
        content = content,
        highlight = lerp(fill, content, 0.26f),
        outline = lerp(fill, content, 0.48f),
    )
}
