package ge.yet.game.uikit.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Partial MiniApp color override.
 *
 * Copies the current [MaterialTheme] scheme with only the demonstrated roles replaced.
 * Every other role (surfaces, containers, tertiary, error, outlines, inverse, …)
 * inherits exactly, so paired surface/onSurface roles stay legible by construction.
 *
 * When changing [background] or [primary] to a different brightness, supply the
 * matching [onBackground]/[onPrimary] as well. Do not add typography, shapes or
 * motion here; the host applies [MaterialTheme] typography/shapes around the result.
 */
@Composable
fun gameColors(
    background: Color = MaterialTheme.colorScheme.background,
    onBackground: Color = MaterialTheme.colorScheme.onBackground,

    primary: Color = MaterialTheme.colorScheme.primary,
    onPrimary: Color = MaterialTheme.colorScheme.onPrimary,
): ColorScheme = MaterialTheme.colorScheme.copy(
    background = background,
    onBackground = onBackground,

    primary = primary,
    onPrimary = onPrimary,
)
