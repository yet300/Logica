package ge.yet.game.miniapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import ge.yet.game.miniapp.compose.MiniAppAdGate
import ge.yet.game.miniapp.compose.MiniAppAdKind
import ge.yet.game.miniapp.compose.MiniAppAdsCapability
import ge.yet.game.monetization.ads.LocalMonetizationState
import ge.yet.game.monetization.ads.rememberFullscreenInterstitial
import ge.yet.game.monetization.core.once

@SingleIn(AppScope::class)
@Inject
internal class AdMobMiniAppAdsCapability : MiniAppAdsCapability {
    @Composable
    override fun rememberGate(kind: MiniAppAdKind): MiniAppAdGate {
        val state = LocalMonetizationState.current
        return when (kind) {
            is MiniAppAdKind.Fullscreen -> {
                val presenter = rememberFullscreenInterstitial()
                remember(state.canShowAds, presenter, kind.reason) {
                    miniAppAdGate(state.canShowAds, presenter)
                }
            }
            // Banner eligibility only: the host owns mounting, sizing and the
            // zero-space-when-empty policy and renders its banner itself when
            // the session opts in via MiniAppSession.wantsBanner.
            MiniAppAdKind.Banner -> remember(state.canShowAds) {
                MiniAppAdGate(
                    willShowAd = state.canShowAds,
                    request = { onComplete -> onComplete() },
                )
            }
        }
    }
}

internal fun miniAppAdGate(
    canShowAds: Boolean,
    presenter: (onComplete: () -> Unit) -> Unit,
): MiniAppAdGate = MiniAppAdGate(
    willShowAd = canShowAds,
    request = { completion ->
        val completeOnce = once(completion)
        if (canShowAds) presenter(completeOnce) else completeOnce()
    },
)
