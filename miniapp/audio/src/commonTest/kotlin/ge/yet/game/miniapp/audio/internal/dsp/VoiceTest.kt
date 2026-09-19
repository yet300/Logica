package ge.yet.game.miniapp.audio.internal.dsp

import ge.yet.game.miniapp.audio.MidiNote
import ge.yet.game.miniapp.audio.OscillatorShape
import ge.yet.game.miniapp.audio.audioProgram
import ge.yet.game.miniapp.audio.hz
import ge.yet.game.miniapp.audio.ms
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class VoiceTest {
    @Test
    fun `partial envelope decays without silencing the fundamental`() {
        fun render(withPartial: Boolean): FloatArray {
            val instrument = audioProgram {
                instrument("mallet") {
                    oscillator(OscillatorShape.SINE, gain = 0.1f)
                    if (withPartial) {
                        partial(ratio = 4f, gain = 0.4f) {
                            envelope(attack = 0.ms, decay = 20.ms, sustain = 0f, release = 0.ms)
                        }
                    }
                }
            }.instruments.single()
            return FloatArray(512).also {
                VoiceState(instrument, MidiNote.of(57), 8_000, it.size).render(it, it.size)
            }
        }

        val fundamental = render(false)
        val layered = render(true)
        val earlyPartial = differenceRms(layered, fundamental, 0, 120)
        val latePartial = differenceRms(layered, fundamental, 320, 512)

        assertTrue(earlyPartial > 0.05f)
        assertTrue(latePartial < 0.001f)
        assertTrue(rms(layered, 320, 512) > 0.02f)
    }

    @Test
    fun `voice waits for partial release tail`() {
        val instrument = audioProgram {
            instrument("tail") {
                partial(ratio = 2f, gain = 0.3f) {
                    envelope(attack = 0.ms, sustain = 1f, release = 20.ms)
                }
                envelope(attack = 0.ms, sustain = 1f, release = 0.ms)
            }
        }.instruments.single()
        val state = VoiceState(instrument, MidiNote.of(60), 8_000, 128)

        state.noteOff()
        state.render(FloatArray(128), 128)
        assertTrue(!state.isFinished)
        state.render(FloatArray(64), 64)
        assertTrue(state.isFinished)
    }

    @Test
    fun `fixed voice pipeline renders sources envelope filter and effects in order`() {
        val instrument = audioProgram {
            instrument("lead") {
                oscillator(OscillatorShape.SAW, gain = 0.7f)
                partial(ratio = 2f, gain = 0.15f)
                envelope(attack = 4.ms, decay = 8.ms, sustain = 0.6f, release = 4.ms)
                lowPass(2_000.hz)
                distortion(0.1f)
                bitCrush(bitDepth = 12, sampleRateReduction = 2)
            }
        }.instruments.single()
        val state = VoiceState(instrument, MidiNote.of(69), sampleRate = 8_000, blockCapacity = 512)
        val first = FloatArray(512)
        val second = FloatArray(512)

        state.render(first, first.size)
        state.render(second, second.size)

        assertTrue((first + second).all { it.isFinite() && abs(it) <= 1.1f })
        assertTrue(first.take(16).zipWithNext().any { (a, b) -> abs(b) > abs(a) })
        assertTrue(first.any { abs(it) > 0.05f })
        assertTrue(!first.contentEquals(second), "voice state must continue across blocks")
    }

    @Test
    fun `frequency modulation and vibrato alter the carrier deterministically`() {
        fun render(modulated: Boolean): FloatArray {
            val instrument = audioProgram {
                instrument("lead") {
                    oscillator(OscillatorShape.SINE)
                    if (modulated) {
                        frequencyModulation(ratio = 2f, index = 1f)
                        vibrato(rate = 5.hz, depthCents = 12f)
                    }
                }
            }.instruments.single()
            return FloatArray(1_000).also {
                VoiceState(instrument, MidiNote.of(69), 8_000, it.size).render(it, it.size)
            }
        }

        val plain = render(false)
        val first = render(true)
        val second = render(true)

        assertTrue(first.contentEquals(second))
        assertTrue(!first.contentEquals(plain))
    }
}

private fun differenceRms(
    left: FloatArray,
    right: FloatArray,
    start: Int,
    endExclusive: Int,
): Float {
    var sum = 0.0
    for (index in start until endExclusive) {
        val difference = left[index] - right[index]
        sum += difference * difference
    }
    return kotlin.math.sqrt(sum / (endExclusive - start)).toFloat()
}

private fun rms(samples: FloatArray, start: Int, endExclusive: Int): Float {
    var sum = 0.0
    for (index in start until endExclusive) sum += samples[index] * samples[index]
    return kotlin.math.sqrt(sum / (endExclusive - start)).toFloat()
}
