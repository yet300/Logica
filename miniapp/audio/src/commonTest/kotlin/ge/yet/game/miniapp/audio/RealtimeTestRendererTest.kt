package ge.yet.game.miniapp.audio

import ge.yet.game.miniapp.audio.testing.AudioTestRenderResult
import ge.yet.game.miniapp.audio.testing.ExperimentalMiniAppAudioTestingApi
import ge.yet.game.miniapp.audio.testing.MiniAppAudioTestRenderer
import ge.yet.game.pattern.sequence
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalMiniAppAudioTestingApi::class)
class RealtimeTestRendererTest {
    @Test
    fun `tiny sine program has stable metrics frequency and hash`() {
        val program = audioProgram {
            tempo(240f)
            instrument("sine") {
                oscillator(OscillatorShape.SINE, gain = 0.5f)
                envelope(attack = 1.ms, sustain = 1f, release = 1.ms)
            }
            musicTrack("tone") {
                instrument("sine")
                notes(MidiNote.of(69))
            }
        }

        val first = render(program, 8_000)
        val second = render(program, 8_000)

        assertEquals(8_000, first.frameCount)
        assertTrue(first.peak in 0.3f..0.6f)
        assertTrue(first.rms in 0.2..0.5)
        assertTrue(abs(dominantCorrelationFrequency(first.left, 8_000) - 440) <= 2)
        assertEquals(first.quantizedPcmHash, second.quantizedPcmHash)
    }

    @Test
    fun `rest events remain silent while pitched events render`() {
        val pattern = sequence<AudioNote>(listOf(AudioNote.Rest, AudioNote.Pitched(MidiNote.of(60))))
        val program = audioProgram {
            tempo(240f)
            instrument("lead") { oscillator(OscillatorShape.SQUARE, gain = 0.25f) }
            musicTrack("line") { instrument("lead"); notes(pattern) }
        }

        val pcm = render(program, 8_000)

        assertTrue(pcm.left.take(4_000).all { it == 0f })
        assertTrue(pcm.left.drop(4_000).any { abs(it) > 0.01f })
    }

    @Test
    fun `invalid program returns diagnostics and no partial audio`() {
        val invalid = audioProgram { instrument("silent") {} }

        val failure = assertIs<AudioTestRenderResult.Failure>(MiniAppAudioTestRenderer.render(invalid, 8_000, 100))

        assertTrue(failure.diagnostics.isNotEmpty())
    }

    @Test
    fun `mapped parameters use the declared control default`() {
        fun renderWithDefault(default: Float) = render(
            audioProgram {
                control("cutoff", default = default, range = 0f..1f)
                instrument("lead") {
                    oscillator(OscillatorShape.SAW, gain = 0.25f)
                    lowPass(control("cutoff").map(100f, 3_000f))
                }
                musicTrack("tone") { instrument("lead"); notes(MidiNote.of(69)) }
            },
            2_000,
        )

        val closed = renderWithDefault(0f)
        val open = renderWithDefault(1f)

        assertTrue(closed.quantizedPcmHash != open.quantizedPcmHash)
        assertTrue(closed.rms < open.rms)
    }

    private fun render(program: AudioProgram, frames: Int) =
        assertIs<AudioTestRenderResult.Success>(MiniAppAudioTestRenderer.render(program, 8_000, frames)).pcm
}

private fun dominantCorrelationFrequency(samples: FloatArray, sampleRate: Int): Int {
    var bestFrequency = 0
    var best = Double.NEGATIVE_INFINITY
    for (frequency in 430..450) {
        var correlation = 0.0
        for (index in samples.indices) correlation += samples[index] * sin(2.0 * PI * frequency * index / sampleRate)
        if (abs(correlation) > best) {
            best = abs(correlation)
            bestFrequency = frequency
        }
    }
    return bestFrequency
}
