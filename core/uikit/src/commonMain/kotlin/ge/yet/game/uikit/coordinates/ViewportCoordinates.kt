package ge.yet.game.uikit.coordinates

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Converts a point measured in window coordinates into viewport-local coordinates.
 *
 * Window and viewport share the same frame of reference; only the viewport origin
 * offset differs, so the conversion is a plain subtraction. Call it at
 * viewport-local rendering boundaries (overlays, effects, hit-testing).
 */
fun windowToViewport(
    pointInWindow: Offset,
    viewportOriginInWindow: Offset,
): Offset = pointInWindow - viewportOriginInWindow

/**
 * Converts bounds measured in window coordinates into viewport-local coordinates.
 */
fun windowToViewport(
    rectInWindow: Rect,
    viewportOriginInWindow: Offset,
): Rect = rectInWindow.translate(-viewportOriginInWindow)
