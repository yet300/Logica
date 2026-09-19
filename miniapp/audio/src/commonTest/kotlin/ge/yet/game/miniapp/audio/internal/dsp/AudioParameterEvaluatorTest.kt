package ge.yet.game.miniapp.audio.internal.dsp

import ge.yet.game.miniapp.audio.smoothNoise
import ge.yet.game.miniapp.audio.noteFrequency
import ge.yet.game.miniapp.audio.hz
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class AudioParameterEvaluatorTest {
    @Test
    fun `note frequency evaluates from the active midi frequency`() {
        val parameter = noteFrequency(ratio = 2f, offsetHz = 10f)

        assertTrue(abs(evaluateAudioParameter(parameter, 0, 8_000, emptyMap(), 110.0) - 230f) < 0.001f)
        assertTrue(abs(evaluateAudioParameter(parameter, 0, 8_000, emptyMap(), 880.0) - 1_770f) < 0.001f)
    }

    @Test
    fun `smooth noise is deterministic continuous and independent of block chunking`() {
        val parameter = smoothNoise(seed = 73, rate = 3.hz, range = -1f..1f)
        val whole = FloatArray(4_000) { frame ->
            evaluateAudioParameter(parameter, frame.toLong(), 1_000, emptyMap())
        }
        val split = FloatArray(4_000)
        for (frame in 0 until 1_337) {
            split[frame] = evaluateAudioParameter(parameter, frame.toLong(), 1_000, emptyMap())
        }
        for (frame in 1_337 until split.size) {
            split[frame] = evaluateAudioParameter(parameter, frame.toLong(), 1_000, emptyMap())
        }

        assertContentEquals(whole, split)
        assertTrue((1 until whole.size).maxOf { abs(whole[it] - whole[it - 1]) } < 0.03f)
    }
}
