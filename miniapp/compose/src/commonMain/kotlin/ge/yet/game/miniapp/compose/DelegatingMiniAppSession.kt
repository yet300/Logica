package ge.yet.game.miniapp.compose

import com.arkivanov.decompose.value.Value

/**
 * Base for sessions bound to one game-owned root component.
 *
 * Covers the three members every session wires identically: the frame mode
 * derived from the active internal child, the host-banner opt-in (passed
 * explicitly so ad-free games never inherit it by accident) and back
 * delegation to the root component. Session-specific chrome
 * ([TopBarContent], [Background], [Content]) stays in the game session.
 *
 * @param onBack delegates to the game-owned root component. It must follow
 * [MiniAppSession.handleBack]: `true` only after internal navigation,
 * `false` on a terminal Result so the host closes the session, stops
 * session audio and unlocks the catalog.
 */
abstract class DelegatingMiniAppSession(
    override val frameMode: Value<MiniAppFrameMode>,
    override val wantsBanner: Boolean,
    private val onBack: () -> Boolean = { false },
) : MiniAppSession {
    override fun handleBack(): Boolean = onBack()
}
