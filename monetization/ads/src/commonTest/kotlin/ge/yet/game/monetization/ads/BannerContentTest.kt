package ge.yet.game.monetization.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BannerContentTest {

    @Test
    fun overlay_dismissal_keeps_loaded_creative_presented() {
        val state = BannerPresentationState()

        state.onLoaded()
        state.onOverlayDismissed()

        assertTrue(state.renderable)
    }

    @Test
    fun ineligible_policy_returns_null_without_loading() = runComposeUiTest {
        // Default LocalMonetizationState denies ads: no SDK load may start.
        var composed = false
        var content: (@Composable () -> Unit)? = { error("must stay null") }
        setContent {
            content = rememberBannerContent()
            SideEffect { composed = true }
        }

        waitForIdle()
        assertTrue(composed)
        assertNull(content)
    }
}
