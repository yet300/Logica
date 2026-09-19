package ge.yet.game.miniapp.audio

import ge.yet.game.pattern.Pattern
import kotlin.math.pow

enum class OscillatorShape { SINE, TRIANGLE, SAW, SQUARE, PULSE }
enum class NoiseColor { WHITE, PINK, BROWN }

sealed interface AudioNote {
    data class Pitched(
        val midi: MidiNote,
        val velocity: Float = 1f,
    ) : AudioNote {
        init {
            require(velocity.isFinite() && velocity in 0f..1f) {
                "Note velocity must be finite and in 0..1"
            }
        }
    }
    data object Rest : AudioNote
}

sealed interface AudioParameter {
    val outputRange: ClosedFloatingPointRange<Float>

    @ConsistentCopyVisibility
    data class Constant internal constructor(val value: Float) : AudioParameter {
        override val outputRange: ClosedFloatingPointRange<Float> = value..value
    }

    @ConsistentCopyVisibility
    data class Control internal constructor(
        val name: AudioControlName,
        override val outputRange: ClosedFloatingPointRange<Float>,
    ) : AudioParameter

    @ConsistentCopyVisibility
    data class SineLfo internal constructor(
        val rate: Frequency,
        val phaseCycles: Float,
        override val outputRange: ClosedFloatingPointRange<Float>,
    ) : AudioParameter

    @ConsistentCopyVisibility
    data class SmoothNoise internal constructor(
        val seed: Long,
        val rate: Frequency,
        override val outputRange: ClosedFloatingPointRange<Float>,
    ) : AudioParameter

    @ConsistentCopyVisibility
    data class NoteFrequency internal constructor(
        val ratio: Float,
        val offsetHz: Float,
        override val outputRange: ClosedFloatingPointRange<Float>,
    ) : AudioParameter

    @ConsistentCopyVisibility
    data class Product internal constructor(
        val left: AudioParameter,
        val right: AudioParameter,
        override val outputRange: ClosedFloatingPointRange<Float>,
    ) : AudioParameter
}

fun sineLfo(
    rate: Frequency,
    range: ClosedFloatingPointRange<Float>,
    phaseCycles: Float = 0f,
): AudioParameter {
    requireValidParameterRange(range)
    require(phaseCycles.isFinite())
    val normalizedPhase = phaseCycles - kotlin.math.floor(phaseCycles)
    return AudioParameter.SineLfo(rate, normalizedPhase, range.start..range.endInclusive)
}

fun smoothNoise(
    seed: Long,
    rate: Frequency,
    range: ClosedFloatingPointRange<Float>,
): AudioParameter {
    requireValidParameterRange(range)
    return AudioParameter.SmoothNoise(seed, rate, range.start..range.endInclusive)
}

fun noteFrequency(
    ratio: Float = 1f,
    offsetHz: Float = 0f,
): AudioParameter {
    require(ratio.isFinite() && ratio > 0f) { "A note-frequency ratio must be finite and positive" }
    require(offsetHz.isFinite()) { "A note-frequency offset must be finite" }
    val minimum = (midiFrequency(0) * ratio + offsetHz).toFloat()
    val maximum = (midiFrequency(127) * ratio + offsetHz).toFloat()
    require(minimum.isFinite() && maximum.isFinite() && minimum > 0f) {
        "A note-frequency range must remain finite and positive"
    }
    return AudioParameter.NoteFrequency(ratio, offsetHz, minimum..maximum)
}

operator fun AudioParameter.times(other: AudioParameter): AudioParameter {
    val products = listOf(
        outputRange.start * other.outputRange.start,
        outputRange.start * other.outputRange.endInclusive,
        outputRange.endInclusive * other.outputRange.start,
        outputRange.endInclusive * other.outputRange.endInclusive,
    )
    require(products.all(Float::isFinite))
    return AudioParameter.Product(this, other, products.min()..products.max())
}

private fun requireValidParameterRange(range: ClosedFloatingPointRange<Float>) {
    require(range.start.isFinite() && range.endInclusive.isFinite() && range.start <= range.endInclusive)
}

private fun midiFrequency(midi: Int): Double =
    440.0 * 2.0.pow((midi - 69) / 12.0)

class AudioControlReference internal constructor(private val name: AudioControlName) {
    fun map(outputStart: Float, outputEndInclusive: Float): AudioParameter {
        require(outputStart.isFinite() && outputEndInclusive.isFinite() && outputStart <= outputEndInclusive)
        return AudioParameter.Control(name, outputStart..outputEndInclusive)
    }
}

fun audioParameter(value: Float): AudioParameter {
    require(value.isFinite())
    return AudioParameter.Constant(value)
}

@ConsistentCopyVisibility
data class AudioControlDeclaration internal constructor(
    val name: AudioControlName,
    val default: Float,
    val range: ClosedFloatingPointRange<Float>,
)

@ConsistentCopyVisibility
data class OscillatorDeclaration internal constructor(
    val shape: OscillatorShape,
    val gain: Gain,
    val detuneCents: Float,
)

@ConsistentCopyVisibility
data class NoiseDeclaration internal constructor(
    val color: NoiseColor,
    val gain: Gain,
    val seed: Long,
)

@ConsistentCopyVisibility
data class AdditivePartialDeclaration internal constructor(
    val ratio: Float,
    val gain: Gain,
    val envelope: EnvelopeDeclaration?,
)

@ConsistentCopyVisibility
data class FrequencyModulationDeclaration internal constructor(
    val ratio: Float,
    val index: Float,
)

@ConsistentCopyVisibility
data class VibratoDeclaration internal constructor(
    val rate: Frequency,
    val depthCents: Float,
)

sealed interface FilterDeclaration {
    val frequency: AudioParameter
    val resonance: Float

    @ConsistentCopyVisibility
    data class LowPass internal constructor(
        override val frequency: AudioParameter,
        override val resonance: Float,
    ) : FilterDeclaration

    @ConsistentCopyVisibility
    data class HighPass internal constructor(
        override val frequency: AudioParameter,
        override val resonance: Float,
    ) : FilterDeclaration

    @ConsistentCopyVisibility
    data class BandPass internal constructor(
        override val frequency: AudioParameter,
        override val resonance: Float,
    ) : FilterDeclaration
}

sealed interface VoiceEffectDeclaration {
    @ConsistentCopyVisibility
    data class Distortion internal constructor(val amount: Float) : VoiceEffectDeclaration

    @ConsistentCopyVisibility
    data class BitCrush internal constructor(
        val bitDepth: Int,
        val sampleRateReduction: Int,
    ) : VoiceEffectDeclaration
}

sealed interface BusEffectDeclaration {
    @ConsistentCopyVisibility
    data class Delay internal constructor(
        val time: AudioDuration,
        val feedback: Float,
    ) : BusEffectDeclaration, SendEffectDeclaration

    @ConsistentCopyVisibility
    data class Reverb internal constructor(val send: Float) : BusEffectDeclaration, SendEffectDeclaration

    @ConsistentCopyVisibility
    data class Compressor internal constructor(
        val threshold: Float,
        val ratio: Float,
        val attack: AudioDuration,
        val release: AudioDuration,
        val makeupGain: Float,
    ) : BusEffectDeclaration

    @ConsistentCopyVisibility
    data class Limiter internal constructor(
        val ceiling: Float,
        val release: AudioDuration,
    ) : BusEffectDeclaration
}

sealed interface SendEffectDeclaration : BusEffectDeclaration

@ConsistentCopyVisibility
data class AudioBusDeclaration internal constructor(
    val effects: List<BusEffectDeclaration>,
)

@ConsistentCopyVisibility
data class EnvelopeDeclaration internal constructor(
    val attack: AudioDuration,
    val decay: AudioDuration,
    val sustain: Float,
    val release: AudioDuration,
)

@ConsistentCopyVisibility
data class PitchDeclaration internal constructor(
    val from: Frequency,
    val to: Frequency,
    val duration: AudioDuration,
)

@ConsistentCopyVisibility
data class InstrumentDeclaration internal constructor(
    val name: InstrumentName,
    val oscillators: List<OscillatorDeclaration>,
    val noises: List<NoiseDeclaration>,
    val partials: List<AdditivePartialDeclaration>,
    val envelope: EnvelopeDeclaration?,
    val frequencyModulation: FrequencyModulationDeclaration?,
    val vibrato: VibratoDeclaration?,
    val filters: List<FilterDeclaration>,
    val effects: List<VoiceEffectDeclaration>,
)

@ConsistentCopyVisibility
data class MusicTrackDeclaration internal constructor(
    val name: MusicTrackName,
    val instrument: InstrumentName,
    val pattern: Pattern<AudioNote>,
    val gain: AudioParameter,
    val pan: AudioParameter,
    val effects: List<SendEffectDeclaration>,
    val sections: List<MusicSectionDeclaration>,
)

@ConsistentCopyVisibility
data class MusicSectionDeclaration internal constructor(
    val cycles: Int,
    val pattern: Pattern<AudioNote>?,
    val transposeSemitones: Int,
)

@ConsistentCopyVisibility
data class SoundEffectDeclaration internal constructor(
    val name: SfxName,
    val oscillators: List<OscillatorDeclaration>,
    val noises: List<NoiseDeclaration>,
    val partials: List<AdditivePartialDeclaration>,
    val envelope: EnvelopeDeclaration?,
    val pitch: PitchDeclaration?,
    val frequencyModulation: FrequencyModulationDeclaration?,
    val vibrato: VibratoDeclaration?,
    val filters: List<FilterDeclaration>,
    val effects: List<VoiceEffectDeclaration>,
)
