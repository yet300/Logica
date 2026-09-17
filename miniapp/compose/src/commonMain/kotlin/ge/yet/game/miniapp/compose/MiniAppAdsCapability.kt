package ge.yet.game.miniapp.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * Generic kinds of advertising a MiniApp may request through [MiniAppAdsCapability].
 *
 * The hierarchy is intentionally closed and game-agnostic. Never add game-specific
 * subtypes or values here (no per-game placements): game semantics travel only in
 * the opaque [Fullscreen.reason], which the host must never branch on.
 */
sealed interface MiniAppAdKind {

    /**
     * A fullscreen interstitial shown at a natural transition.
     *
     * @param reason opaque, game-local description of the transition
     * (for example `"continue_after_game_over"`). Free-form: the host passes it
     * through and never interprets it.
     */
    data class Fullscreen(val reason: String) : MiniAppAdKind

    /**
     * Banner eligibility declaration.
     *
     * The game only declares opt-in; mounting, sizing, safe-area handling and
     * the zero-space-when-empty policy stay host-owned. The host renders its
     * banner only when the session opts in (see `MiniAppSession.wantsBanner`)
     * and a renderable creative exists.
     */
    data object Banner : MiniAppAdKind
}

@Immutable
data class MiniAppAdGate(
    val willShowAd: Boolean,
    val request: (onComplete: () -> Unit) -> Unit,
)

interface MiniAppAdsCapability {

    @Composable
    fun rememberGate(kind: MiniAppAdKind): MiniAppAdGate
}
