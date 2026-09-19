package ge.yet.game.miniapp.api

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether a fullscreen interstitial is currently on screen.
 *
 * Lives in the stable API module (not in `:monetization:*`) so both the ad
 * capability (producer) and the audio engine (consumer) depend inward on
 * contracts, never sideways on each other's modules.
 *
 * The host ad capability enters before the native presentation and exits on
 * every terminal callback (dismissed, failure, or immediate completion), so
 * audio layers can fade game sound out for the exact ad window and resume it
 * afterwards, uniformly on every platform — including iOS, where presenting
 * an ad never backgrounds the app and visibility alone cannot observe it.
 *
 * Counter-based: overlapping gate requests cannot clear each other's signal.
 * Every `enterFullscreenAd` pairs with the single `exitFullscreenAd` of the
 * same gate request completion. Confined to the main thread like all other
 * UI-driven ad callbacks.
 */
@SingleIn(AppScope::class)
class FullscreenAdVisibility @Inject constructor() {
    private var activeCount = 0
    private val _showing = MutableStateFlow(false)
    val showing: StateFlow<Boolean> = _showing.asStateFlow()

    fun enterFullscreenAd() {
        activeCount += 1
        _showing.value = true
    }

    fun exitFullscreenAd() {
        activeCount = (activeCount - 1).coerceAtLeast(0)
        _showing.value = activeCount > 0
    }
}
