package ge.yet.game.screen.miniapp

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * App-internal sink for the resolved session background.
 *
 * Set once by the Android shell; invoked from common host code with the active
 * session's resolved `colors.background` so native system-icon appearance can
 * follow the app theme instead of the device theme. Host-only: game modules
 * must not depend on `:composeApp` (enforced by dependency validation), so
 * games can never observe or provide this.
 */
val LocalSystemChromeReporter =
    staticCompositionLocalOf<((Color) -> Unit)?> { null }

/**
 * Chooses dark system icons when black contrasts more against [background]
 * than white does, light icons otherwise. Pure function over the resolved
 * semantic background color; never samples composable pixels.
 */
fun shouldUseDarkSystemIcons(background: Color): Boolean {
    val luminance = background.luminance()
    val contrastWithBlack = (luminance + 0.05f) / 0.05f
    val contrastWithWhite = 1.05f / (luminance + 0.05f)
    return contrastWithBlack >= contrastWithWhite
}

fun opaqueBackground(background: Color, base: Color): Color =
    if (background.alpha >= 1f) background else background.compositeOver(base).copy(alpha = 1f)
