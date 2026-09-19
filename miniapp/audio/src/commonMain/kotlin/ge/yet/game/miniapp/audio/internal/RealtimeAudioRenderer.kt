package ge.yet.game.miniapp.audio.internal

import ge.yet.game.miniapp.audio.AudioControlName
import ge.yet.game.miniapp.audio.AudioMobileBudget
import ge.yet.game.miniapp.audio.BusEffectDeclaration
import ge.yet.game.miniapp.audio.CompiledAudioProgram
import ge.yet.game.miniapp.audio.MidiNote
import ge.yet.game.miniapp.audio.MusicTrackDeclaration
import ge.yet.game.miniapp.audio.SfxName
import ge.yet.game.miniapp.audio.internal.dsp.SmoothedGainState
import ge.yet.game.miniapp.audio.internal.dsp.VoiceState
import ge.yet.game.miniapp.audio.internal.dsp.DelayState
import ge.yet.game.miniapp.audio.internal.dsp.DynamicsState
import ge.yet.game.miniapp.audio.internal.dsp.ReverbState
import ge.yet.game.miniapp.audio.internal.dsp.applyCompressorStereo
import ge.yet.game.miniapp.audio.internal.dsp.applyDelay
import ge.yet.game.miniapp.audio.internal.dsp.applyLimiterStereo
import ge.yet.game.miniapp.audio.internal.dsp.applyReverb
import ge.yet.game.miniapp.audio.internal.dsp.applySmoothedGain
import ge.yet.game.miniapp.audio.internal.dsp.limitStereo
import ge.yet.game.miniapp.audio.internal.dsp.mixMonoToStereo
import ge.yet.game.miniapp.audio.internal.dsp.mixMonoToStereoAutomated
import kotlin.math.ln
import kotlin.math.roundToInt

/** Shared PCM block renderer. Platform sinks own its thread confinement. */
internal class RealtimeAudioRenderer(
    private val sampleRate: Int,
    private val blockCapacity: Int,
) : AudioRuntimeCommandTarget {
    private val controlPositions = RealtimeControlPositions(AudioMobileBudget.MAX_CONTROLS)
    private val voiceAllocator = VoiceAllocator()
    private val voiceSlots = Array(AudioMobileBudget.MAX_VOICES) {
        RealtimeVoiceSlot(
            state = VoiceState(sampleRate, blockCapacity, controlPositions),
            scratch = FloatArray(blockCapacity),
        )
    }
    private val leftPolicyGain = SmoothedGainState(1f)
    private val rightPolicyGain = SmoothedGainState(1f)
    private val leftStopGain = SmoothedGainState(1f)
    private val rightStopGain = SmoothedGainState(1f)
    private val musicLeft = FloatArray(blockCapacity)
    private val musicRight = FloatArray(blockCapacity)
    private val sfxLeft = FloatArray(blockCapacity)
    private val sfxRight = FloatArray(blockCapacity)
    private val trackBuffers = Array(AudioMobileBudget.MAX_TRACKS) { FloatArray(blockCapacity) }
    private val musicDelayStates = Array(AudioMobileBudget.MAX_DELAY_PROCESSORS) {
        DelayState((AudioMobileBudget.MAX_DELAY_SECONDS * sampleRate).roundToInt())
    }
    private val musicReverbStates = Array(AudioMobileBudget.MAX_REVERB_PROCESSORS) { ReverbState(sampleRate) }
    private val musicDynamicsStates = Array(AudioMobileBudget.MAX_DYNAMICS_PROCESSORS) { DynamicsState() }
    private val sfxDelayStates = Array(AudioMobileBudget.MAX_DELAY_PROCESSORS) {
        DelayState((AudioMobileBudget.MAX_DELAY_SECONDS * sampleRate).roundToInt())
    }
    private val sfxReverbStates = Array(AudioMobileBudget.MAX_REVERB_PROCESSORS) { ReverbState(sampleRate) }
    private val sfxDynamicsStates = Array(AudioMobileBudget.MAX_DYNAMICS_PROCESSORS) { DynamicsState() }
    private val scheduler = AudioScheduler(sampleRate)
    private var program: CompiledAudioProgram? = null
    private var sfxProgram: CompiledAudioProgram? = null
    private var framePosition = 0L
    private var policy = AudioSessionPolicy.Active
    private var stopAfterFade = false
    private var stopFadeFrames = 0
    private var lastVoiceAllocationStole = false

    val hasActiveAudio: Boolean
        get() {
            if (program != null) return true
            for (index in voiceSlots.indices) {
                val slot = voiceSlots[index]
                if (slot.active && slot.kind == VoiceKind.SFX) return true
            }
            return false
        }

    internal val voiceStateAllocationCount: Int get() = voiceSlots.size
    internal val scratchBufferAllocationCount: Int get() = voiceSlots.size
    internal val schedulerAllocationCount: Int get() = 1
    internal val trackBufferAllocationCount: Int get() = trackBuffers.size
    internal val effectStateAllocationCount: Int
        get() = musicDelayStates.size + musicReverbStates.size + musicDynamicsStates.size +
            sfxDelayStates.size + sfxReverbStates.size + sfxDynamicsStates.size

    init {
        require(sampleRate in 8_000..192_000)
        require(blockCapacity > 0)
    }

    fun updatePolicy(value: AudioSessionPolicy) {
        policy = value
    }

    fun render(left: FloatArray, right: FloatArray, frameCount: Int) {
        require(frameCount in 0..blockCapacity)
        require(left.size >= frameCount && right.size >= frameCount)
        left.fill(0f, 0, frameCount)
        right.fill(0f, 0, frameCount)
        musicLeft.fill(0f, 0, frameCount)
        musicRight.fill(0f, 0, frameCount)
        sfxLeft.fill(0f, 0, frameCount)
        sfxRight.fill(0f, 0, frameCount)
        if (policy.schedulingPaused || frameCount == 0) return

        val activeProgram = program
        if (activeProgram != null) {
            scheduleNewMusicVoices(activeProgram, frameCount)
            renderMusicVoices(activeProgram, frameCount)
            processMusic(activeProgram, frameCount)
            framePosition += frameCount
        }
        val stopTarget = if (stopAfterFade) 0f else 1f
        applySmoothedGain(musicLeft, stopTarget, stopFadeFrames, leftStopGain, frameCount)
        applySmoothedGain(musicRight, stopTarget, stopFadeFrames, rightStopGain, frameCount)
        applySmoothedGain(musicLeft, policy.musicGain, POLICY_RAMP_FRAMES, leftPolicyGain, frameCount)
        applySmoothedGain(musicRight, policy.musicGain, POLICY_RAMP_FRAMES, rightPolicyGain, frameCount)
        if (stopAfterFade && leftStopGain.remaining == 0 && leftStopGain.current == 0f) clearMusic()
        for (frame in 0 until frameCount) {
            left[frame] = musicLeft[frame]
            right[frame] = musicRight[frame]
        }
        renderSfxVoices(sfxLeft, sfxRight, frameCount)
        sfxProgram?.let { processStereoBus(it.source.sfxBus.effects, sfxLeft, sfxRight, frameCount, false) }
        for (frame in 0 until frameCount) {
            left[frame] += sfxLeft[frame]
            right[frame] += sfxRight[frame]
        }
        limitStereo(left, right, frameCount)
    }

    override fun playMusic(program: CompiledAudioProgram): AudioRuntimeCommandOutcome {
        clearMusic()
        this.program = program
        scheduler.reset(program)
        check(controlPositions.reset(program.source.controls))
        resetGain(leftPolicyGain, policy.musicGain)
        resetGain(rightPolicyGain, policy.musicGain)
        resetGain(leftStopGain, 1f)
        resetGain(rightStopGain, 1f)
        resetMusicEffectStates()
        return AudioRuntimeCommandOutcome.APPLIED
    }

    override fun stopMusic(fadeFrames: Int): AudioRuntimeCommandOutcome {
        if (fadeFrames == 0) {
            clearMusic()
        } else {
            stopAfterFade = true
            stopFadeFrames = fadeFrames
        }
        return AudioRuntimeCommandOutcome.APPLIED
    }

    override fun playSfx(program: CompiledAudioProgram, name: SfxName): AudioRuntimeCommandOutcome {
        val soundEffectIndex = program.soundEffectIndex(name)
        if (soundEffectIndex < 0) return AudioRuntimeCommandOutcome.VALIDATION_REJECTED
        val declaration = program.source.soundEffects[soundEffectIndex]
        if (sfxProgram !== program) {
            sfxProgram = program
            resetSfxEffectStates()
        }
        val slotIndex = allocateVoice(VoiceKind.SFX)
        if (slotIndex < 0) return AudioRuntimeCommandOutcome.VALIDATION_REJECTED
        val slot = voiceSlots[slotIndex]
        val instrument = program.soundEffectInstrumentAt(soundEffectIndex)
        val frequency = declaration.pitch?.from?.value ?: DEFAULT_SFX_FREQUENCY
        val midi = (MIDI_A4 + MIDI_NOTES_PER_OCTAVE * ln(frequency / DEFAULT_SFX_FREQUENCY) / ln(2.0))
            .roundToInt()
            .coerceIn(MIDI_MIN, MIDI_MAX)
        slot.state.reset(instrument, MidiNote.of(midi), pitch = declaration.pitch)
        slot.track = null
        slot.remainingFrames = 0L
        slot.blockOffset = 0
        slot.releaseStartFrame = ((declaration.pitch?.duration?.seconds ?: DEFAULT_SFX_HOLD_SECONDS) * sampleRate)
            .roundToInt()
            .coerceAtLeast(1)
        slot.renderedFrames = 0L
        slot.releasing = false
        return if (lastVoiceAllocationStole) {
            AudioRuntimeCommandOutcome.FORCED_VOICE_SHEDDING
        } else {
            AudioRuntimeCommandOutcome.APPLIED
        }
    }

    override fun setControl(name: AudioControlName, value: Float): AudioRuntimeCommandOutcome {
        val declaration = program?.source?.controls?.firstOrNull { it.name == name }
            ?: return AudioRuntimeCommandOutcome.VALIDATION_REJECTED
        val width = declaration.range.endInclusive - declaration.range.start
        check(controlPositions.set(name, if (width == 0f) 0f else (value - declaration.range.start) / width))
        return AudioRuntimeCommandOutcome.APPLIED
    }

    override fun destroy(): AudioRuntimeCommandOutcome {
        clearMusic()
        for (index in voiceSlots.indices) {
            val slot = voiceSlots[index]
            if (slot.active) finishVoice(slot)
        }
        return AudioRuntimeCommandOutcome.APPLIED
    }

    private fun scheduleNewMusicVoices(program: CompiledAudioProgram, frameCount: Int) {
        val scheduled = scheduler.scheduleBlockInto(framePosition, frameCount)
        for (index in 0 until scheduled.size) {
            val track = program.source.musicTracks[scheduled.trackIndexAt(index)]
            val instrument = program.source.instruments.first { it.name == track.instrument }
            val slotIndex = allocateVoice(VoiceKind.MUSIC)
            if (slotIndex < 0) continue
            val slot = voiceSlots[slotIndex]
            slot.state.reset(instrument, scheduled.noteAt(index), velocity = scheduled.velocityAt(index))
            slot.track = track
            slot.trackIndex = scheduled.trackIndexAt(index)
            slot.remainingFrames = scheduled.durationFramesAt(index)
            slot.blockOffset = scheduled.frameOffsetAt(index)
            slot.releaseStartFrame = 0
            slot.renderedFrames = 0L
            slot.releasing = false
        }
    }

    private fun renderMusicVoices(program: CompiledAudioProgram, frameCount: Int) {
        for (trackIndex in program.source.musicTracks.indices) {
            trackBuffers[trackIndex].fill(0f, 0, frameCount)
        }
        for (index in voiceSlots.indices) {
            val slot = voiceSlots[index]
            if (!slot.active || slot.kind != VoiceKind.MUSIC) continue
            val availableFrames = frameCount - slot.blockOffset
            val renderedFrames = minOf(slot.remainingFrames, availableFrames.toLong()).toInt()
            if (renderedFrames > 0) {
                slot.scratch.fill(0f, 0, frameCount)
                slot.state.render(slot.scratch, renderedFrames, slot.blockOffset)
                val trackBuffer = trackBuffers[slot.trackIndex]
                for (frame in 0 until frameCount) trackBuffer[frame] += slot.scratch[frame]
                slot.remainingFrames -= renderedFrames
            }
            slot.blockOffset = 0
            if (slot.remainingFrames <= 0) finishVoice(slot)
        }
    }

    private fun processMusic(program: CompiledAudioProgram, frameCount: Int) {
        var delayIndex = 0
        var reverbIndex = 0
        for (trackIndex in program.source.musicTracks.indices) {
            val track = program.source.musicTracks[trackIndex]
            val cursor = processMonoEffects(
                trackBuffers[trackIndex], track.effects, frameCount,
                musicDelayStates, delayIndex, musicReverbStates, reverbIndex,
            )
            delayIndex = cursor ushr 16
            reverbIndex = cursor and 0xffff
            mixMonoToStereoAutomated(
                mono = trackBuffers[trackIndex], left = musicLeft, right = musicRight,
                frameCount = frameCount, sampleRate = sampleRate, gain = track.gain, pan = track.pan,
                controlPositions = controlPositions, absoluteStartFrame = framePosition,
            )
        }
        processStereoBus(
            program.source.musicBus.effects, musicLeft, musicRight, frameCount, true,
            delayIndex, reverbIndex,
        )
    }

    private fun processMonoEffects(
        buffer: FloatArray,
        effects: List<ge.yet.game.miniapp.audio.SendEffectDeclaration>,
        frameCount: Int,
        delays: Array<DelayState>,
        initialDelayIndex: Int,
        reverbs: Array<ReverbState>,
        initialReverbIndex: Int,
    ): Int {
        var delayIndex = initialDelayIndex
        var reverbIndex = initialReverbIndex
        for (index in effects.indices) when (val effect = effects[index]) {
            is BusEffectDeclaration.Delay -> {
                applyDelay(buffer, (effect.time.seconds * sampleRate).roundToInt().coerceAtLeast(1), effect.feedback, 1f, delays[delayIndex], frameCount)
                delayIndex += 1
            }
            is BusEffectDeclaration.Reverb -> {
                applyReverb(buffer, effect.send, reverbs[reverbIndex], frameCount)
                reverbIndex += 1
            }
        }
        return (delayIndex shl 16) or reverbIndex
    }

    private fun processStereoBus(
        effects: List<BusEffectDeclaration>,
        left: FloatArray,
        right: FloatArray,
        frameCount: Int,
        music: Boolean,
        initialDelayIndex: Int = 0,
        initialReverbIndex: Int = 0,
    ) {
        val delays = if (music) musicDelayStates else sfxDelayStates
        val reverbs = if (music) musicReverbStates else sfxReverbStates
        val dynamics = if (music) musicDynamicsStates else sfxDynamicsStates
        var delayIndex = initialDelayIndex
        var reverbIndex = initialReverbIndex
        var dynamicsIndex = 0
        for (index in effects.indices) when (val effect = effects[index]) {
            is BusEffectDeclaration.Delay -> {
                val frames = (effect.time.seconds * sampleRate).roundToInt().coerceAtLeast(1)
                applyDelay(left, frames, effect.feedback, 1f, delays[delayIndex], frameCount)
                applyDelay(right, frames, effect.feedback, 1f, delays[delayIndex + 1], frameCount)
                delayIndex += 2
            }
            is BusEffectDeclaration.Reverb -> {
                applyReverb(left, effect.send, reverbs[reverbIndex], frameCount)
                applyReverb(right, effect.send, reverbs[reverbIndex + 1], frameCount)
                reverbIndex += 2
            }
            is BusEffectDeclaration.Compressor -> {
                applyCompressorStereo(left, right, effect.threshold, effect.ratio, effect.attack.seconds, effect.release.seconds, effect.makeupGain, sampleRate, dynamics[dynamicsIndex], frameCount)
                dynamicsIndex += 1
            }
            is BusEffectDeclaration.Limiter -> {
                applyLimiterStereo(left, right, effect.ceiling, effect.release.seconds, sampleRate, dynamics[dynamicsIndex], frameCount)
                dynamicsIndex += 1
            }
        }
    }

    private fun renderSfxVoices(left: FloatArray, right: FloatArray, frameCount: Int) {
        for (index in voiceSlots.indices) {
            val slot = voiceSlots[index]
            if (!slot.active || slot.kind != VoiceKind.SFX) continue
            if (!slot.releasing && slot.renderedFrames >= slot.releaseStartFrame) {
                slot.state.noteOff()
                voiceAllocator.markReleased(slot.voiceId)
                slot.releasing = true
            }
            slot.scratch.fill(0f, 0, frameCount)
            slot.state.render(slot.scratch, frameCount)
            mixMonoToStereo(slot.scratch, left, right, frameCount, gain = 1f, pan = 0f)
            slot.renderedFrames += frameCount
            if (slot.state.isFinished) finishVoice(slot)
        }
    }

    private fun allocateVoice(kind: VoiceKind): Int {
        if (!voiceAllocator.allocateRealtime(kind, framePosition)) return -1
        val stolenVoiceId = voiceAllocator.lastStolenVoiceId
        val slotIndex = if (stolenVoiceId != 0L) findVoiceSlot(stolenVoiceId) else findFreeVoiceSlot()
        check(slotIndex >= 0) { "Voice allocator and realtime slots are out of sync" }
        val slot = voiceSlots[slotIndex]
        slot.active = true
        slot.voiceId = voiceAllocator.lastAllocatedVoiceId
        slot.kind = kind
        lastVoiceAllocationStole = stolenVoiceId != 0L
        return slotIndex
    }

    private fun findVoiceSlot(voiceId: Long): Int {
        for (index in voiceSlots.indices) {
            val slot = voiceSlots[index]
            if (slot.active && slot.voiceId == voiceId) return index
        }
        return -1
    }

    private fun findFreeVoiceSlot(): Int {
        for (index in voiceSlots.indices) if (!voiceSlots[index].active) return index
        return -1
    }

    private fun finishVoice(slot: RealtimeVoiceSlot) {
        check(voiceAllocator.finish(slot.voiceId)) { "Voice allocator and realtime slots are out of sync" }
        slot.active = false
        slot.voiceId = 0L
        slot.kind = null
        slot.track = null
        slot.trackIndex = -1
        slot.remainingFrames = 0L
        slot.blockOffset = 0
        slot.releaseStartFrame = 0
        slot.renderedFrames = 0L
        slot.releasing = false
    }

    private fun clearMusic() {
        program = null
        for (index in voiceSlots.indices) {
            val slot = voiceSlots[index]
            if (slot.active && slot.kind == VoiceKind.MUSIC) finishVoice(slot)
        }
        controlPositions.clear()
        framePosition = 0L
        stopAfterFade = false
        stopFadeFrames = 0
    }


    private fun resetMusicEffectStates() {
        for (state in musicDelayStates) state.reset()
        for (state in musicReverbStates) state.reset()
        for (state in musicDynamicsStates) state.reset()
    }

    private fun resetSfxEffectStates() {
        for (state in sfxDelayStates) state.reset()
        for (state in sfxReverbStates) state.reset()
        for (state in sfxDynamicsStates) state.reset()
    }

    private class RealtimeVoiceSlot(
        val state: VoiceState,
        val scratch: FloatArray,
        var active: Boolean = false,
        var voiceId: Long = 0L,
        var kind: VoiceKind? = null,
        var track: MusicTrackDeclaration? = null,
        var trackIndex: Int = -1,
        var remainingFrames: Long = 0L,
        var blockOffset: Int = 0,
        var releaseStartFrame: Int = 0,
        var renderedFrames: Long = 0L,
        var releasing: Boolean = false,
    )

}

private fun resetGain(state: SmoothedGainState, value: Float) {
    state.current = value
    state.target = value
    state.step = 0f
    state.remaining = 0
}

private const val POLICY_RAMP_FRAMES = 128
private const val DEFAULT_SFX_FREQUENCY = 440.0
private const val DEFAULT_SFX_HOLD_SECONDS = 0.05
private const val MIDI_A4 = 69.0
private const val MIDI_NOTES_PER_OCTAVE = 12.0
private const val MIDI_MIN = 0
private const val MIDI_MAX = 127
