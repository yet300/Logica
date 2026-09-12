package ge.yet.game.screen.miniapp

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SystemChromeTest {

    @Test
    fun light_background_selects_dark_icons() {
        assertTrue(shouldUseDarkSystemIcons(Color.White))
        assertTrue(shouldUseDarkSystemIcons(Color(0xFFF5F4ED)))
    }

    @Test
    fun dark_background_selects_light_icons() {
        assertFalse(shouldUseDarkSystemIcons(Color.Black))
        assertFalse(shouldUseDarkSystemIcons(Color(0xFF0E0E0D)))
    }

    @Test
    fun translucent_background_is_resolved_to_opaque() {
        val resolved = opaqueBackground(Color.Red.copy(alpha = 0.5f), Color.White)

        assertEquals(1f, resolved.alpha)
    }
}
