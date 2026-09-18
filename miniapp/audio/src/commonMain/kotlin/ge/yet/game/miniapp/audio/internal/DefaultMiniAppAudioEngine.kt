package ge.yet.game.miniapp.audio.internal

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import ge.yet.game.domain.repository.CrashlyticsRepository
import ge.yet.game.domain.repository.SettingsRepository
import ge.yet.game.miniapp.api.FullscreenAdVisibility
import ge.yet.game.miniapp.api.MiniAppId
import ge.yet.game.miniapp.api.MiniAppVisibilitySource
import ge.yet.game.miniapp.audio.MiniAppAudio
import ge.yet.game.miniapp.audio.MiniAppAudioEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

internal class DefaultMiniAppAudioEngine(
    private val appScope: CoroutineScope,
    private val settings: SettingsRepository,
    private val adVisibility: FullscreenAdVisibility,
    private val sink: PlatformAudioSink,
    crashlytics: CrashlyticsRepository,
) : MiniAppAudioEngine {
    private val diagnostics = AudioDiagnostics(crashlytics)
    private val active = MutableStateFlow<ActiveAudioSession?>(null)

    override fun openSession(
        id: MiniAppId,
        sessionKey: Long,
        lifecycle: Lifecycle,
        visibility: MiniAppVisibilitySource,
    ): MiniAppAudio {
        active.value?.audio?.close()
        lateinit var audio: DefaultMiniAppAudio
        audio = DefaultMiniAppAudio(
            backendFactory = {
                try {
                    sink.openSession(id, sessionKey)
                } catch (error: Exception) {
                    diagnostics.backendFailure(error)
                    null
                }
            },
            diagnostics = diagnostics,
            initialVisibility = visibility.visibility.value,
            initialMusicEnabled = settings.musicEnabled.value,
            initialSfxEnabled = settings.sfxEnabled.value,
            onClosed = { clearIfActive(id, sessionKey, audio) },
        )
        active.value = ActiveAudioSession(id, sessionKey, audio)
        audio.attachVisibilityJob(
            appScope.launch {
                visibility.visibility.collect(audio::updateVisibility)
            },
        )
        // One fade supervisor per session: collectLatest cancels a fade
        // mid-flight when the signal toggles, and the next edge ramps from
        // the live gain, so rapid show/dismiss never jumps. Music fades out
        // smoothly and pauses (zero CPU, scheduler keeps its position for a
        // mid-track resume); SFX cut instantly; dismiss reverses everything.
        // Visibility transitions compose underneath through the shared state.
        audio.attachAdJob(
            appScope.launch {
                var wasShowing = false
                adVisibility.showing.collectLatest { showing ->
                    if (showing == wasShowing) return@collectLatest
                    wasShowing = showing
                    if (showing) {
                        audio.setAdSuppressed(true)
                        fadeMusicGain(audio, target = 0f)
                        audio.setAdPaused(true)
                    } else {
                        audio.setAdPaused(false)
                        fadeMusicGain(audio, target = null)
                        audio.setAdSuppressed(false)
                    }
                }
            },
        )
        lifecycle.doOnDestroy { closeSession(id, sessionKey) }
        return audio
    }

    override fun closeSession(id: MiniAppId, sessionKey: Long) {
        val session = active.value?.takeIf { it.id == id && it.sessionKey == sessionKey } ?: return
        session.audio.close()
    }

    suspend fun observeSettings() {
        combine(settings.musicEnabled, settings.sfxEnabled, ::Pair).collect { (music, sfx) ->
            active.value?.audio?.updateSettings(music, sfx)
        }
    }

    suspend fun reportDiagnostics(periodMillis: Long = DIAGNOSTIC_PERIOD_MILLIS) {
        require(periodMillis > 0)
        while (coroutineContext.isActive) {
            delay(periodMillis)
            drainDiagnostics()
        }
    }

    fun drainDiagnostics() {
        active.value?.audio?.drainDiagnostics()
    }

    private fun clearIfActive(id: MiniAppId, sessionKey: Long, audio: DefaultMiniAppAudio) {
        active.compareAndSet(ActiveAudioSession(id, sessionKey, audio), null)
    }

    /**
     * Ramps the session music gain toward [target] in fixed steps. A null
     * target releases override authority back to visibility/settings after
     * ramping to the live base gain.
     */
    private suspend fun fadeMusicGain(audio: DefaultMiniAppAudio, target: Float?) {
        val start = audio.effectiveMusicGain()
        val end = target ?: audio.baseMusicGain()
        repeat(AD_FADE_STEPS) { step ->
            val fraction = (step + 1).toFloat() / AD_FADE_STEPS
            audio.setMusicGainOverride((start + (end - start) * fraction).coerceIn(0f, 1f))
            delay(AD_FADE_STEP_MS)
        }
        if (target == null) audio.setMusicGainOverride(null)
    }

    private data class ActiveAudioSession(
        val id: MiniAppId,
        val sessionKey: Long,
        val audio: DefaultMiniAppAudio,
    )
}

private const val DIAGNOSTIC_PERIOD_MILLIS = 15_000L
private const val AD_FADE_STEPS = 4
private const val AD_FADE_STEP_MS = 50L
