package ge.yet.game.blockblast.audio

import ge.yet.game.miniapp.audio.SfxName
import ge.yet.game.miniapp.audio.testing.AudioTestPcm
import ge.yet.game.miniapp.audio.testing.AudioTestRenderRequest
import ge.yet.game.miniapp.audio.testing.AudioTestRenderResult
import ge.yet.game.miniapp.audio.testing.AudioTestSfxTrigger
import ge.yet.game.miniapp.audio.testing.ExperimentalMiniAppAudioTestingApi
import ge.yet.game.miniapp.audio.testing.MiniAppAudioTestRenderer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalMiniAppAudioTestingApi::class)
class BlockBlastAudioRenderTest {
    @Test
    fun `placement thock keeps its approved acoustic signature`() {
        val placement = renderSfx(BlockBlastAudio.Place)

        assertEquals(-7588597565231800787L, placement.quantizedPcmHash)
        assertTrue(placement.peak < MAX_PEAK)
        assertTrue(placement.rms > MIN_AUDIBLE_RMS)
        assertEquals(SFX_FRAME_COUNT, placement.frameCount)
    }

    @Test
    fun `program compiles inside mobile budgets on the realtime render path`() {
        val pcm = renderSuccess(
            AudioTestRenderRequest(
                sampleRate = SAMPLE_RATE,
                frameCount = SPECTRAL_WINDOW_FRAMES,
            ),
        )

        assertEquals(SAMPLE_RATE, pcm.sampleRate)
        assertEquals(SPECTRAL_WINDOW_FRAMES, pcm.frameCount)
    }

    @Test
    fun `program without music renders silence until an sfx triggers`() {
        val silent = renderSuccess(
            AudioTestRenderRequest(
                sampleRate = SAMPLE_RATE,
                frameCount = SPECTRAL_WINDOW_FRAMES,
            ),
        )

        assertTrue(silent.left.all { it == 0f })
        assertTrue(silent.right.all { it == 0f })

        val voiced = renderSfx(BlockBlastAudio.Place)
        assertTrue(voiced.left.any { it != 0f })
    }

    @Test
    fun `every authored voice is deterministic finite audible stereo and below clipping`() {
        APPROVED_SFX.forEach { name ->
            val first = renderSfx(name)
            val second = renderSfx(name)
            val firstLeft = first.left
            val firstRight = first.right

            assertEquals(first.quantizedPcmHash, second.quantizedPcmHash, name.value)
            assertContentEquals(firstLeft, second.left, name.value)
            assertContentEquals(firstRight, second.right, name.value)
            assertTrue(firstLeft.all(Float::isFinite), "${name.value}: left PCM must be finite")
            assertTrue(firstRight.all(Float::isFinite), "${name.value}: right PCM must be finite")
            assertTrue(first.rms > MIN_AUDIBLE_RMS, "${name.value}: RMS ${first.rms}")
            assertTrue(channelRms(firstLeft) > MIN_CHANNEL_RMS, "${name.value}: left channel is silent")
            assertTrue(channelRms(firstRight) > MIN_CHANNEL_RMS, "${name.value}: right channel is silent")
            assertTrue(first.peak < MAX_PEAK, "${name.value}: peak ${first.peak}")
        }
    }

    @Test
    fun `voice tiers keep increasing spectral centroids`() {
        val good = spectralCentroid(renderSfx(BlockBlastAudio.VoiceGood))
        val great = spectralCentroid(renderSfx(BlockBlastAudio.VoiceGreat))
        val amazing = spectralCentroid(renderSfx(BlockBlastAudio.VoiceAmazing))
        val excellent = spectralCentroid(renderSfx(BlockBlastAudio.VoiceExcellent))
        val unbelievable = spectralCentroid(renderSfx(BlockBlastAudio.VoiceUnbelievable))

        assertTrue(good < great, "Expected voice_good centroid $good below voice_great $great")
        assertTrue(great < amazing, "Expected voice_great centroid $great below voice_amazing $amazing")
        assertTrue(amazing < excellent, "Expected voice_amazing centroid $amazing below voice_excellent $excellent")
        assertTrue(
            excellent < unbelievable,
            "Expected voice_excellent centroid $excellent below voice_unbelievable $unbelievable",
        )
    }

    @Test
    fun `unbelievable rises in windowed pitch`() {
        val unbelievable = renderSfx(BlockBlastAudio.VoiceUnbelievable)
        val early = dominantFrequency(unbelievable, startFrame = 256, windowFrames = PITCH_WINDOW_FRAMES)
        val late = dominantFrequency(unbelievable, startFrame = 3_200, windowFrames = PITCH_WINDOW_FRAMES)

        assertTrue(early < late, "Expected Unbelievable to rise: $early -> $late")
    }

    @Test
    fun `clear pop rises while game over falls in windowed pitch`() {
        val clearPop = renderSfx(BlockBlastAudio.ClearPop)
        val clearEarly = dominantFrequency(clearPop, startFrame = 64, windowFrames = CLEAR_PITCH_WINDOW_FRAMES)
        val clearLate = dominantFrequency(clearPop, startFrame = 640, windowFrames = CLEAR_PITCH_WINDOW_FRAMES)
        val gameOver = renderSfx(BlockBlastAudio.GameOver)
        val gameOverEarly = dominantFrequency(gameOver, startFrame = 256, windowFrames = PITCH_WINDOW_FRAMES)
        val gameOverLate = dominantFrequency(gameOver, startFrame = 3_200, windowFrames = PITCH_WINDOW_FRAMES)

        assertTrue(clearEarly < clearLate, "Expected Clear Pop to rise: $clearEarly -> $clearLate")
        assertTrue(gameOverEarly > gameOverLate, "Expected Game Over to fall: $gameOverEarly -> $gameOverLate")
    }

    @Test
    fun `rapid ordered place clear and voice mix stays deterministic and bounded`() {
        fun render(vararg triggers: AudioTestSfxTrigger): AudioTestPcm = renderSuccess(
            AudioTestRenderRequest(
                sampleRate = SAMPLE_RATE,
                frameCount = SFX_FRAME_COUNT,
                includeMusic = false,
                sfxTriggers = triggers.toList(),
            ),
        )
        val place = AudioTestSfxTrigger(BlockBlastAudio.Place, frameOffset = 0)
        val clear = AudioTestSfxTrigger(BlockBlastAudio.ClearPop, frameOffset = CLEAR_OFFSET)
        val voice = AudioTestSfxTrigger(BlockBlastAudio.VoiceGreat, frameOffset = VOICE_OFFSET)

        val placeOnly = render(place)
        val placeAndClear = render(place, clear)
        val full = render(place, clear, voice)
        val fullAgain = render(place, clear, voice)

        assertStereoPrefixEquals(placeOnly, placeAndClear, CLEAR_OFFSET, "clear boundary")
        assertStereoDiffersAfter(placeOnly, placeAndClear, CLEAR_OFFSET, "clear trigger")
        assertStereoPrefixEquals(placeAndClear, full, VOICE_OFFSET, "voice boundary")
        assertStereoDiffersAfter(placeAndClear, full, VOICE_OFFSET, "voice trigger")
        assertDeterministic("rapid place clear voice", full, fullAgain)
        assertPcmHealth("rapid place clear voice", full)
    }

    private fun renderSfx(name: SfxName): AudioTestPcm = renderSuccess(
        AudioTestRenderRequest(
            sampleRate = SAMPLE_RATE,
            frameCount = SFX_FRAME_COUNT,
            includeMusic = false,
            sfxTriggers = listOf(AudioTestSfxTrigger(name, frameOffset = 0)),
        ),
    )

    private fun renderSuccess(request: AudioTestRenderRequest): AudioTestPcm {
        val result = MiniAppAudioTestRenderer.render(BlockBlastAudio.program, request)
        return assertIs<AudioTestRenderResult.Success>(result).pcm
    }

    private fun assertDeterministic(label: String, first: AudioTestPcm, second: AudioTestPcm) {
        assertEquals(first.quantizedPcmHash, second.quantizedPcmHash, label)
        assertContentEquals(first.left, second.left, label)
        assertContentEquals(first.right, second.right, label)
    }

    private fun assertStereoPrefixEquals(
        expected: AudioTestPcm,
        actual: AudioTestPcm,
        endExclusive: Int,
        label: String,
    ) {
        assertContentEquals(expected.left.copyOfRange(0, endExclusive), actual.left.copyOfRange(0, endExclusive), label)
        assertContentEquals(
            expected.right.copyOfRange(0, endExclusive),
            actual.right.copyOfRange(0, endExclusive),
            label,
        )
    }

    private fun assertStereoDiffersAfter(
        before: AudioTestPcm,
        after: AudioTestPcm,
        startInclusive: Int,
        label: String,
    ) {
        val beforeLeft = before.left
        val beforeRight = before.right
        val afterLeft = after.left
        val afterRight = after.right
        assertTrue(
            (startInclusive until before.frameCount).any { frame -> beforeLeft[frame] != afterLeft[frame] },
            "$label must change left samples at or after frame $startInclusive",
        )
        assertTrue(
            (startInclusive until before.frameCount).any { frame -> beforeRight[frame] != afterRight[frame] },
            "$label must change right samples at or after frame $startInclusive",
        )
    }

    private fun assertPcmHealth(label: String, pcm: AudioTestPcm) {
        val left = pcm.left
        val right = pcm.right
        assertTrue(left.all(Float::isFinite), "$label: left PCM must be finite")
        assertTrue(right.all(Float::isFinite), "$label: right PCM must be finite")
        assertTrue(channelRms(left) > MIN_CHANNEL_RMS, "$label: left channel is silent")
        assertTrue(channelRms(right) > MIN_CHANNEL_RMS, "$label: right channel is silent")
        assertTrue(pcm.peak < MAX_PEAK, "$label: peak ${pcm.peak}")
    }

    private fun channelRms(samples: FloatArray): Double {
        var sum = 0.0
        for (sample in samples) sum += sample * sample
        return sqrt(sum / samples.size)
    }

    private fun spectralCentroid(pcm: AudioTestPcm): Double {
        val samples = monoWindow(pcm, startFrame = 0, windowFrames = SPECTRAL_WINDOW_FRAMES)
        var weightedFrequencies = 0.0
        var magnitudes = 0.0
        for (bin in 1..samples.size / 2) {
            val magnitude = dftMagnitude(samples, bin)
            val frequency = bin.toDouble() * pcm.sampleRate / samples.size
            weightedFrequencies += frequency * magnitude
            magnitudes += magnitude
        }
        return weightedFrequencies / magnitudes
    }

    private fun dominantFrequency(
        pcm: AudioTestPcm,
        startFrame: Int,
        windowFrames: Int,
    ): Double {
        val samples = monoWindow(pcm, startFrame, windowFrames)
        val minimumBin = (MIN_PITCH_HZ * samples.size / pcm.sampleRate).toInt().coerceAtLeast(1)
        val maximumBin = (MAX_PITCH_HZ * samples.size / pcm.sampleRate).toInt()
            .coerceAtMost(samples.size / 2)
        var dominantBin = minimumBin
        var dominantMagnitude = Double.NEGATIVE_INFINITY
        for (bin in minimumBin..maximumBin) {
            val magnitude = dftMagnitude(samples, bin)
            if (magnitude > dominantMagnitude) {
                dominantMagnitude = magnitude
                dominantBin = bin
            }
        }
        return dominantBin.toDouble() * pcm.sampleRate / samples.size
    }

    private fun monoWindow(
        pcm: AudioTestPcm,
        startFrame: Int,
        windowFrames: Int,
    ): DoubleArray {
        require(startFrame >= 0 && startFrame + windowFrames <= pcm.frameCount)
        val left = pcm.left
        val right = pcm.right
        return DoubleArray(windowFrames) { frame ->
            val hann = 0.5 - 0.5 * cos(2.0 * PI * frame / (windowFrames - 1))
            ((left[startFrame + frame] + right[startFrame + frame]) * 0.5) * hann
        }
    }

    private fun dftMagnitude(samples: DoubleArray, bin: Int): Double {
        var real = 0.0
        var imaginary = 0.0
        val radiansPerFrame = 2.0 * PI * bin / samples.size
        for (frame in samples.indices) {
            val phase = radiansPerFrame * frame
            real += samples[frame] * cos(phase)
            imaginary -= samples[frame] * kotlin.math.sin(phase)
        }
        return sqrt(real * real + imaginary * imaginary)
    }

    private companion object {
        val APPROVED_SFX = listOf(
            BlockBlastAudio.Place,
            BlockBlastAudio.ClearPop,
            BlockBlastAudio.GameOver,
            BlockBlastAudio.Revive,
            BlockBlastAudio.NewBest,
            BlockBlastAudio.VoiceGood,
            BlockBlastAudio.VoiceGreat,
            BlockBlastAudio.VoiceAmazing,
            BlockBlastAudio.VoiceExcellent,
            BlockBlastAudio.VoiceUnbelievable,
        )
        const val SAMPLE_RATE = 16_000
        const val SFX_FRAME_COUNT = 16_000
        const val SPECTRAL_WINDOW_FRAMES = 2_048
        const val PITCH_WINDOW_FRAMES = 768
        const val CLEAR_PITCH_WINDOW_FRAMES = 512
        const val CLEAR_OFFSET = 64
        const val VOICE_OFFSET = 128
        const val MIN_PITCH_HZ = 70.0
        const val MAX_PITCH_HZ = 2_000.0
        const val MIN_AUDIBLE_RMS = 0.0001
        const val MIN_CHANNEL_RMS = 0.00005
        const val MAX_PEAK = 0.95f
    }
}
