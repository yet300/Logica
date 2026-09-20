package ge.yet.game.fallingblocks.ui.board

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import ge.yet.game.fallingblocks.domain.model.Tetromino
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TetrominoStyleTest {
    @Test
    fun `every tetromino maps to a distinct Material color role`() {
        val scheme = lightColorScheme(
            primary = Color(0xFF010101),
            secondary = Color(0xFF020202),
            tertiary = Color(0xFF030303),
            primaryContainer = Color(0xFF040404),
            secondaryContainer = Color(0xFF050505),
            tertiaryContainer = Color(0xFF060606),
            inversePrimary = Color(0xFF070707),
        )

        val fills = Tetromino.entries.map { it.colors(scheme).fill }

        assertEquals(Tetromino.entries.size, fills.distinct().size)
        assertEquals(scheme.primary, Tetromino.I.colors(scheme).fill)
        assertEquals(scheme.inversePrimary, Tetromino.Z.colors(scheme).fill)
    }

    @Test
    fun `piece colors follow the current theme`() {
        val light = Tetromino.T.colors(lightColorScheme())
        val dark = Tetromino.T.colors(darkColorScheme())

        assertNotEquals(light.fill, dark.fill)
        assertNotEquals(light.outline, dark.outline)
    }
}
