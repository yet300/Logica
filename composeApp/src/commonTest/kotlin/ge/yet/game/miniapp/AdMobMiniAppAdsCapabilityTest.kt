package ge.yet.game.miniapp

import ge.yet.game.miniapp.api.FullscreenAdVisibility
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdMobMiniAppAdsCapabilityTest {

    @Test
    fun `ineligible ads complete without presenting`() {
        var completions = 0
        val gate = miniAppAdGate(
            canShowAds = false,
            presenter = { error("presenter must not run") },
        )

        gate.request { completions += 1 }

        assertFalse(gate.willShowAd)
        assertEquals(1, completions)
    }

    @Test
    fun `eligible ads delegate and preserve exactly once completion`() {
        var sdkCompletion: (() -> Unit)? = null
        var completions = 0
        val gate = miniAppAdGate(true) { sdkCompletion = it }

        gate.request { completions += 1 }
        val completion = assertNotNull(sdkCompletion)
        completion()
        completion()

        assertTrue(gate.willShowAd)
        assertEquals(1, completions)
    }

    @Test
    fun `visibility brackets the request and clears on completion`() {
        val adVisibility = FullscreenAdVisibility()
        var sdkCompletion: (() -> Unit)? = null
        var completions = 0
        val gate = miniAppAdGate(true) { sdkCompletion = it }
            .withFullscreenAdVisibility(adVisibility)

        gate.request { completions += 1 }
        assertTrue(adVisibility.showing.value)

        val completion = assertNotNull(sdkCompletion)
        completion()
        assertEquals(1, completions)
        assertFalse(adVisibility.showing.value)
    }

    @Test
    fun `visibility clears on immediate completion without presenting`() {
        val adVisibility = FullscreenAdVisibility()
        var completions = 0
        val gate = miniAppAdGate(
            canShowAds = false,
            presenter = { error("presenter must not run") },
        ).withFullscreenAdVisibility(adVisibility)

        gate.request { completions += 1 }

        assertEquals(1, completions)
        assertFalse(adVisibility.showing.value)
    }
}
