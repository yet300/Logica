package ge.yet.game.fallingblocks.audio

import ge.yet.game.miniapp.audio.SfxName
import ge.yet.game.miniapp.audio.testing.AudioTestControlValue
import ge.yet.game.miniapp.audio.testing.AudioTestPcm
import ge.yet.game.miniapp.audio.testing.AudioTestRenderRequest
import ge.yet.game.miniapp.audio.testing.AudioTestRenderResult
import ge.yet.game.miniapp.audio.testing.AudioTestSfxTrigger
import ge.yet.game.miniapp.audio.testing.ExperimentalMiniAppAudioTestingApi
import ge.yet.game.miniapp.audio.testing.MiniAppAudioTestRenderer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalMiniAppAudioTestingApi::class)
class FallingBlocksAudioRenderTest {
    @Test
    fun `program keeps 126 BPM and the complete typed SFX contract`() {
        assertEquals(126f, FallingBlocksProgram.tempo.bpm)
        assertEquals(
            FallingBlocksAudio.allSfx.mapTo(linkedSetOf()) { it.value },
            FallingBlocksProgram.soundEffects.mapTo(linkedSetOf()) { it.name.value },
        )
        assertEquals(
            listOf("bass_grid", "pulse_grid", "lead_fragments"),
            FallingBlocksProgram.musicTracks.map { it.name.value },
        )
    }

    @Test
    fun `music is deterministic audible bounded and responds to intensity`() {
        val low = renderMusic(0.18f)
        val lowAgain = renderMusic(0.18f)
        val high = renderMusic(0.90f)

        assertHealthy("low intensity", low)
        assertHealthy("high intensity", high)
        assertEquals(low.quantizedPcmHash, lowAgain.quantizedPcmHash)
        assertContentEquals(low.left, lowAgain.left)
        assertContentEquals(low.right, lowAgain.right)
        assertNotEquals(low.quantizedPcmHash, high.quantizedPcmHash)
        assertTrue(high.rms > low.rms, "High intensity should add audible energy: ${low.rms} -> ${high.rms}")
    }

    @Test
    fun `every SFX is deterministic finite audible and below clipping`() {
        FallingBlocksAudio.allSfx.forEach { name ->
            val first = renderSfx(name)
            val second = renderSfx(name)

            assertHealthy(name.value, first)
            assertEquals(first.quantizedPcmHash, second.quantizedPcmHash, name.value)
            assertContentEquals(first.left, second.left, name.value)
            assertContentEquals(first.right, second.right, name.value)
        }
    }

    private fun renderMusic(intensity: Float): AudioTestPcm = render(
        AudioTestRenderRequest(
            sampleRate = SAMPLE_RATE,
            frameCount = MUSIC_FRAMES,
            controls = listOf(AudioTestControlValue(FallingBlocksAudio.Intensity, intensity)),
        ),
    )

    private fun renderSfx(name: SfxName): AudioTestPcm = render(
        AudioTestRenderRequest(
            sampleRate = SAMPLE_RATE,
            frameCount = SFX_FRAMES,
            includeMusic = false,
            sfxTriggers = listOf(AudioTestSfxTrigger(name, frameOffset = 0)),
        ),
    )

    private fun render(request: AudioTestRenderRequest): AudioTestPcm = assertIs<AudioTestRenderResult.Success>(
        MiniAppAudioTestRenderer.render(FallingBlocksProgram, request),
    ).pcm

    private fun assertHealthy(label: String, pcm: AudioTestPcm) {
        assertTrue(pcm.left.all(Float::isFinite), "$label: left PCM must be finite")
        assertTrue(pcm.right.all(Float::isFinite), "$label: right PCM must be finite")
        assertTrue(pcm.rms > MIN_AUDIBLE_RMS, "$label: RMS ${pcm.rms}")
        assertTrue(pcm.peak < MAX_PEAK, "$label: peak ${pcm.peak}")
    }

    private companion object {
        const val SAMPLE_RATE = 24_000
        const val MUSIC_FRAMES = 24_000
        const val SFX_FRAMES = 16_000
        const val MIN_AUDIBLE_RMS = 0.0001
        const val MAX_PEAK = 0.98f
    }
}
