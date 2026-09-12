package ge.yet.game.monetization.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.lexilabs.basic.ads.AdSize
import app.lexilabs.basic.ads.AdUnitId
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.composable.BannerAd
import app.lexilabs.basic.ads.composable.rememberBannerAd

internal class BannerPresentationState {
    var renderable by mutableStateOf(false)
        private set

    fun onLoaded() {
        renderable = true
    }

    fun onFailure() {
        renderable = false
    }

    fun onOverlayDismissed() = Unit
}

/**
 * Nullable loaded-banner content for the host frame.
 *
 * Returns null until a renderable native creative exists, so a null slot
 * consumes zero ad layout space. Loading starts here (outside the returned
 * slot), therefore a banner can finish loading while nothing is mounted.
 *
 * The loaded latch survives impression/click/overlay callbacks; a failed
 * explicit load clears it because explicit loads only run when nothing is
 * mounted (NONE/DISMISSED). Policy revocation returns null and drops the
 * composition-scoped handler; mounted-view lifecycle (pause/resume/destroy)
 * stays owned by the SDK's BannerAd composable, and provisional loads that
 * never mount share the SDK's pre-existing handler lifetime (no worse than
 * the previous always-mounted implementation).
 */
@OptIn(DependsOnGoogleMobileAds::class)
@Composable
fun rememberBannerContent(): (@Composable () -> Unit)? {
    if (!LocalMonetizationState.current.canShowAds) return null
    val configuration = checkNotNull(LocalAdMobConfiguration.current) {
        "rememberBannerContent must be used inside AdMobProvider"
    }
    val adUnitId = AdUnitId.autoSelect(
        androidAdUnitId = configuration.bannerAndroidUnitId,
        iosAdUnitId = configuration.bannerIosUnitId,
    )
    val presentation = remember(adUnitId) { BannerPresentationState() }
    val bannerAd by rememberBannerAd(
        adUnitId = adUnitId,
        adSize = AdSize.BANNER,
        onLoad = presentation::onLoaded,
        onFailure = { presentation.onFailure() },
        onDismissed = { presentation.onOverlayDismissed() },
    )
    if (!presentation.renderable) return null
    return remember(bannerAd) {
        {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BannerAd(ad = bannerAd)
            }
        }
    }
}
