package ge.yet.game.miniapp.audio.internal

import ge.yet.game.miniapp.api.MiniAppVisibility
import ge.yet.game.miniapp.audio.AudioCommandRejection
import ge.yet.game.miniapp.audio.AudioCommandResult
import ge.yet.game.miniapp.audio.AudioCompilationResult
import ge.yet.game.miniapp.audio.AudioControlName
import ge.yet.game.miniapp.audio.AudioDuration
import ge.yet.game.miniapp.audio.AudioLookupResult
import ge.yet.game.miniapp.audio.AudioProgram
import ge.yet.game.miniapp.audio.CompiledAudioProgram
import ge.yet.game.miniapp.audio.MiniAppAudio
import ge.yet.game.miniapp.audio.SfxName
import ge.yet.game.miniapp.audio.compile
import ge.yet.game.miniapp.audio.control
import ge.yet.game.miniapp.audio.sfx
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.roundToInt

internal class DefaultMiniAppAudio(
    backendFactory: () -> PlatformAudioSinkSession?,
    private val diagnostics: AudioDiagnostics,
    initialVisibility: MiniAppVisibility,
    initialMusicEnabled: Boolean,
    initialSfxEnabled: Boolean,
    private val onClosed: () -> Unit,
) : MiniAppAudio {
    private val backend = lazy(backendFactory)
    private val state = MutableStateFlow(
        State(
            visibility = initialVisibility,
            musicEnabled = initialMusicEnabled,
            sfxEnabled = initialSfxEnabled,
        ),
    )

    override fun playMusic(program: AudioProgram): AudioCommandResult {
        rejectedWhenUnavailable()?.let { return it }
        val compiled = when (val result = compileCached(program)) {
            is AudioCompilationResult.Failure -> return AudioCommandResult.Rejected(
                AudioCommandRejection.INVALID_PROGRAM,
                result.diagnostics,
            )
            is AudioCompilationResult.Success -> result.program
        }
        return submit { it.playMusic(compiled) }.also { result ->
            if (result === AudioCommandResult.Accepted) {
                updateState { it.copy(currentMusic = program, musicGainOverride = null, adPaused = false) }
                updateBackendPolicy()
            }
        }
    }

    override fun stopMusic(fadeOut: AudioDuration): AudioCommandResult {
        rejectedWhenUnavailable()?.let { return it }
        val activeBackend = existingBackend() ?: return AudioCommandResult.Accepted
        val fadeFrames = (fadeOut.seconds * activeBackend.sampleRate).roundToInt()
        return submit { it.stopMusic(fadeFrames) }.also { result ->
            if (result === AudioCommandResult.Accepted) {
                updateState { it.copy(currentMusic = null, musicGainOverride = null, adPaused = false) }
                updateBackendPolicy()
            }
        }
    }

    override fun playSfx(program: AudioProgram, name: SfxName): AudioCommandResult {
        rejectedWhenUnavailable()?.let { return it }
        if (!policy(state.value).acceptsNewSfx) {
            return AudioCommandResult.Rejected(AudioCommandRejection.PLAYBACK_SUPPRESSED)
        }
        if (program.sfx(name) is AudioLookupResult.Missing) {
            return AudioCommandResult.Rejected(AudioCommandRejection.UNKNOWN_SFX)
        }
        val compiled = when (val result = compileCached(program)) {
            is AudioCompilationResult.Failure -> return AudioCommandResult.Rejected(
                AudioCommandRejection.INVALID_PROGRAM,
                result.diagnostics,
            )
            is AudioCompilationResult.Success -> result.program
        }
        return submit { it.playSfx(compiled, name) }
    }

    override fun setControl(name: AudioControlName, value: Float): AudioCommandResult {
        rejectedWhenUnavailable()?.let { return it }
        val declaration = when (val result = state.value.currentMusic?.control(name)) {
            null, is AudioLookupResult.Missing ->
                return AudioCommandResult.Rejected(AudioCommandRejection.UNKNOWN_CONTROL)
            is AudioLookupResult.Found -> result.value
        }
        if (!value.isFinite() || value !in declaration.range) {
            return AudioCommandResult.Rejected(AudioCommandRejection.CONTROL_OUT_OF_RANGE)
        }
        return submit { it.setControl(name, value) }
    }

    fun attachVisibilityJob(job: Job) {
        while (true) {
            val current = state.value
            if (current.closed) {
                job.cancel()
                return
            }
            if (state.compareAndSet(current, current.copy(visibilityJob = job))) return
        }
    }

    fun attachAdJob(job: Job) {
        while (true) {
            val current = state.value
            if (current.closed) {
                job.cancel()
                return
            }
            if (state.compareAndSet(current, current.copy(adJob = job))) return
        }
    }

    fun updateVisibility(value: MiniAppVisibility) {
        val updated = updateOpenState { current ->
            if (current.visibility == value) current else current.copy(visibility = value)
        } ?: return
        updateBackendPolicy(updated)
    }

    /**
     * Smooth music-gain override for the ad supervisor. Null restores
     * authority to visibility/settings; an absolute value temporarily wins.
     */
    fun setMusicGainOverride(gain: Float?) {
        require(gain == null || (gain.isFinite() && gain in 0f..1f))
        val updated = updateOpenState { current ->
            if (current.musicGainOverride == gain) current else current.copy(musicGainOverride = gain)
        } ?: return
        updateBackendPolicy(updated)
    }

    /**
     * Pauses music scheduling without forgetting the program, so a later
     * unpause resumes mid-track with no recompile.
     */
    fun setAdPaused(paused: Boolean) {
        val updated = updateOpenState { current ->
            if (current.adPaused == paused) current else current.copy(adPaused = paused)
        } ?: return
        updateBackendPolicy(updated)
    }

    /** Effective music gain for fade supervisors to ramp from/to. */
    fun effectiveMusicGain(): Float = policy(state.value).musicGain

    /** Visibility/settings music gain ignoring any supervisor override. */
    fun baseMusicGain(): Float = baseMusicGainOf(state.value)

    /**
     * Suppresses new SFX while a fullscreen ad is showing. Music itself is
     * faded by the supervisor through [setMusicGainOverride]; this only gates
     * commands.
     */
    fun setAdSuppressed(suppressed: Boolean) {
        val updated = updateOpenState { current ->
            if (current.adSuppressed == suppressed) current else current.copy(adSuppressed = suppressed)
        } ?: return
        updateBackendPolicy(updated)
    }

    fun updateSettings(musicEnabled: Boolean, sfxEnabled: Boolean) {
        val updated = updateOpenState { current ->
            if (current.musicEnabled == musicEnabled && current.sfxEnabled == sfxEnabled) {
                current
            } else {
                current.copy(musicEnabled = musicEnabled, sfxEnabled = sfxEnabled)
            }
        } ?: return
        updateBackendPolicy(updated)
    }

    fun drainDiagnostics() {
        if (state.value.closed) return
        try {
            existingBackend()?.drainDiagnostics()?.let(diagnostics::report)
        } catch (error: Exception) {
            diagnostics.backendFailure(error)
        }
    }

    fun close() {
        val previous = closeState() ?: return
        previous.visibilityJob?.cancel()
        previous.adJob?.cancel()
        try {
            existingBackend()?.release()
        } catch (error: Exception) {
            diagnostics.backendFailure(error)
        } finally {
            onClosed()
        }
    }

    private fun rejectedWhenUnavailable(): AudioCommandResult.Rejected? = when {
        state.value.closed -> AudioCommandResult.Rejected(AudioCommandRejection.SESSION_CLOSED)
        else -> null
    }

    private fun updateBackendPolicy(current: State = state.value) {
        try {
            existingBackend()?.updatePolicy(policy(current))
        } catch (error: Exception) {
            diagnostics.backendFailure(error)
        }
    }

    private fun policy(current: State): AudioSessionPolicy {
        val visibilityPolicy = visibilityPolicyOf(current)
        return visibilityPolicy.copy(
            musicGain = current.musicGainOverride ?: baseMusicGainOf(current),
            acceptsNewSfx = current.sfxEnabled && visibilityPolicy.acceptsNewSfx && !current.adSuppressed,
            schedulingPaused = visibilityPolicy.schedulingPaused || current.adPaused,
        )
    }

    private fun visibilityPolicyOf(current: State): AudioSessionPolicy = when (current.visibility) {
        MiniAppVisibility.ACTIVE -> AudioSessionPolicy.Active
        MiniAppVisibility.OBSCURED -> AudioSessionPolicy.Obscured
        MiniAppVisibility.BACKGROUND -> AudioSessionPolicy.Background
    }

    private fun baseMusicGainOf(current: State): Float {
        val visibilityPolicy = visibilityPolicyOf(current)
        return if (current.musicEnabled) visibilityPolicy.musicGain else 0f
    }

    private fun updateState(transform: (State) -> State) {
        while (true) {
            val current = state.value
            if (state.compareAndSet(current, transform(current))) return
        }
    }

    private fun updateOpenState(transform: (State) -> State): State? {
        while (true) {
            val current = state.value
            if (current.closed) return null
            val updated = transform(current)
            if (updated == current) return null
            if (state.compareAndSet(current, updated)) return updated
        }
    }

    private fun closeState(): State? {
        while (true) {
            val current = state.value
            if (current.closed) return null
            if (state.compareAndSet(current, current.copy(closed = true, visibilityJob = null, adJob = null))) return current
        }
    }

    private inline fun submit(operation: (PlatformAudioSinkSession) -> AudioRuntimeSubmitResult): AudioCommandResult =
        try {
            val activeBackend = backend.value
                ?: return AudioCommandResult.Rejected(AudioCommandRejection.BACKEND_UNAVAILABLE)
            if (state.value.closed) {
                try {
                    activeBackend.release()
                } catch (error: Exception) {
                    diagnostics.backendFailure(error)
                }
                return AudioCommandResult.Rejected(AudioCommandRejection.SESSION_CLOSED)
            }
            activeBackend.updatePolicy(policy(state.value))
            when (operation(activeBackend)) {
                AudioRuntimeSubmitResult.Accepted,
                AudioRuntimeSubmitResult.AcceptedAfterEviction,
                AudioRuntimeSubmitResult.Coalesced,
                -> AudioCommandResult.Accepted
                AudioRuntimeSubmitResult.RejectedQueueFull ->
                    AudioCommandResult.Rejected(AudioCommandRejection.COMMAND_QUEUE_FULL)
                AudioRuntimeSubmitResult.RejectedDestroyed ->
                    AudioCommandResult.Rejected(AudioCommandRejection.SESSION_CLOSED)
            }
        } catch (error: Exception) {
            diagnostics.backendFailure(error)
            AudioCommandResult.Rejected(AudioCommandRejection.BACKEND_UNAVAILABLE)
        }

    private fun existingBackend(): PlatformAudioSinkSession? =
        if (backend.isInitialized()) backend.value else null

    private fun compileCached(program: AudioProgram): AudioCompilationResult {
        val current = state.value
        if (current.compiledSource === program) {
            return AudioCompilationResult.Success(requireNotNull(current.compiledProgram))
        }
        val result = program.compile()
        if (result is AudioCompilationResult.Success) {
            updateState { it.copy(compiledSource = program, compiledProgram = result.program) }
        }
        return result
    }

    private data class State(
        val visibility: MiniAppVisibility,
        val musicEnabled: Boolean,
        val sfxEnabled: Boolean,
        val currentMusic: AudioProgram? = null,
        val compiledSource: AudioProgram? = null,
        val compiledProgram: CompiledAudioProgram? = null,
        val visibilityJob: Job? = null,
        val adJob: Job? = null,
        val adSuppressed: Boolean = false,
        val musicGainOverride: Float? = null,
        val adPaused: Boolean = false,
        val closed: Boolean = false,
    )
}
