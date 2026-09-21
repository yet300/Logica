package ge.yet.game.fallingblocks.ui.board

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.compositeOver
import ge.yet.game.fallingblocks.domain.model.Tetromino
import kotlin.test.Test
import kotlin.test.assertTrue

class GhostStyleTest {
    @Test
    fun `ghost separates translucent fill from a readable outline in every theme`() {
        listOf(lightColorScheme(), darkColorScheme()).forEach { scheme ->
            Tetromino.entries.forEach { type ->
                val style = ghostStyle(type, scheme)
                val board = scheme.surfaceContainerLowest

                assertTrue(style.fill.alpha in 0.18f..0.28f)
                assertTrue(style.outline.alpha >= 0.62f)
                assertTrue(
                    contrastRatio(style.outline.compositeOver(board), board) >= 3.0,
                    "$type ghost is not readable against the board",
                )
                assertTrue(style.strokeWidthFraction >= 0.055f)
            }
        }
    }
}
