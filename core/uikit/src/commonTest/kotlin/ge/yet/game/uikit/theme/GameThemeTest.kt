package ge.yet.game.uikit.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class GameThemeTest {

    @Test
    fun game_colors_override_only_declared_roles() = runComposeUiTest {
        var overridden = lightColorScheme()
        var untouchedSurface = Color.Transparent
        var untouchedTertiary = Color.Transparent
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                val base = MaterialTheme.colorScheme
                SideEffect {
                    untouchedSurface = base.surface
                    untouchedTertiary = base.tertiary
                }
                overridden = gameColors(
                    background = Color.Red,
                    onBackground = Color.White,
                    primary = Color.Blue,
                    onPrimary = Color.Black,
                )
            }
        }

        waitForIdle()
        assertEquals(Color.Red, overridden.background)
        assertEquals(Color.White, overridden.onBackground)
        assertEquals(Color.Blue, overridden.primary)
        assertEquals(Color.Black, overridden.onPrimary)
        assertEquals(untouchedSurface, overridden.surface)
        assertEquals(untouchedTertiary, overridden.tertiary)
    }

    @Test
    fun game_colors_default_to_the_current_scheme() = runComposeUiTest {
        var resolved = lightColorScheme()
        var baseBackground = Color.Transparent
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color.Green)) {
                baseBackground = MaterialTheme.colorScheme.background
                resolved = gameColors()
            }
        }

        waitForIdle()
        assertEquals(Color.Green, resolved.primary)
        assertEquals(baseBackground, resolved.background)
    }
}
