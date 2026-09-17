package ge.yet.game.fruitmerge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ge.yet.game.fruitmerge.component.root.RootComponent
import ge.yet.game.fruitmerge.ui.screen.root.RootContent
import ge.yet.game.fruitmerge.ui.FruitMergeTestTags
import ge.yet.game.fruitmerge.ui.MarketStallBackground
import ge.yet.game.fruitmerge.ui.MarketPriceTag
import ge.yet.game.miniapp.compose.DelegatingMiniAppSession
import ge.yet.game.miniapp.compose.MiniAppFrameMode
import ge.yet.game.miniapp.compose.MiniAppAdKind
import ge.yet.game.miniapp.compose.MiniAppAdsCapability

class FruitMergeSession internal constructor(
    private val component: RootComponent,
    private val interstitials: MiniAppAdsCapability,
) : DelegatingMiniAppSession(
    frameMode = component.frameMode,
    wantsBanner = true,
    onBack = component::handleBack,
) {

    @Composable
    override fun TopBarContent() {
        val model by component.game.model.subscribeAsState()
        if (model.initialized) {
            MarketPriceTag(
                score = model.game.score,
                bestScore = model.game.bestScore,
                bestImprovedInRun = model.game.bestImprovedInRun,
                modifier = Modifier.semantics { testTag = FruitMergeTestTags.PriceTag },
            )
        }
    }

    @Composable
    override fun Background(modifier: Modifier) {
        MarketStallBackground(modifier = modifier)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val clearGate = interstitials.rememberGate(MiniAppAdKind.Fullscreen("fruit_merge_clear"))
        val shakeGate = interstitials.rememberGate(MiniAppAdKind.Fullscreen("fruit_merge_shake"))
        RootContent(
            component = component,
            requestClearAd = { token ->
                clearGate.request { component.completePaidAction(token) }
            },
            requestShakeAd = { token ->
                shakeGate.request { component.completePaidAction(token) }
            },
            modifier = modifier,
        )
    }
}

internal enum class FruitMergeBackgroundRole {
    MARKET,
}

internal fun fruitMergeBackgroundRole(@Suppress("UNUSED_PARAMETER") mode: MiniAppFrameMode):
    FruitMergeBackgroundRole = FruitMergeBackgroundRole.MARKET
