package ge.yet.game.miniapp.api

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FullscreenAdVisibilityTest {

    @Test
    fun `enter shows and exit hides`() {
        val visibility = FullscreenAdVisibility()

        assertFalse(visibility.showing.value)
        visibility.enterFullscreenAd()
        assertTrue(visibility.showing.value)
        visibility.exitFullscreenAd()
        assertFalse(visibility.showing.value)
    }

    @Test
    fun `overlapping requests keep showing until every exit`() {
        val visibility = FullscreenAdVisibility()

        visibility.enterFullscreenAd()
        visibility.enterFullscreenAd()
        visibility.exitFullscreenAd()
        assertTrue(visibility.showing.value)
        visibility.exitFullscreenAd()
        assertFalse(visibility.showing.value)
    }

    @Test
    fun `unbalanced exit never goes negative`() {
        val visibility = FullscreenAdVisibility()

        visibility.exitFullscreenAd()
        assertFalse(visibility.showing.value)
        visibility.enterFullscreenAd()
        visibility.exitFullscreenAd()
        visibility.exitFullscreenAd()
        assertFalse(visibility.showing.value)
    }
}
