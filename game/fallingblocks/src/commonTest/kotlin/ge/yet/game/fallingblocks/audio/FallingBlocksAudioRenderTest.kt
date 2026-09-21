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
    fun `program stays inside a bounded mobile declaration budget`() {
        val voiceSources = FallingBlocksProgram.soundEffects.sumOf { effect ->
            effect.oscillators.size + effect.noises.size + effect.partials.size
        }

        assertEquals(1, FallingBlocksProgram.controls.size)
        assertEquals(3, FallingBlocksProgram.musicTracks.size)
        assertTrue(FallingBlocksProgram.instruments.size <= 3)
        assertTrue(FallingBlocksProgram.soundEffects.size <= 16)
        assertTrue(voiceSources <= 40, "Expected at most 40 simultaneous declaration sources, got $voiceSources")
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

    @Test
    fun `line clear tiers are acoustically distinct and stronger within authored families`() {
        val line1 = renderSfx(FallingBlocksAudio.Line1)
        val line2 = renderSfx(FallingBlocksAudio.Line2)
        val line3 = renderSfx(FallingBlocksAudio.Line3)
        val line4 = renderSfx(FallingBlocksAudio.Line4)
        val perfect = renderSfx(FallingBlocksAudio.Perfect)

        assertEquals(5, listOf(line1, line2, line3, line4, perfect).map { it.quantizedPcmHash }.distinct().size)
        assertTrue(line1.rms < line2.rms, "line_1 ${line1.rms} should be quieter than line_2 ${line2.rms}")
        assertTrue(line2.rms < line3.rms, "line_2 ${line2.rms} should be quieter than line_3 ${line3.rms}")
        assertTrue(line4.rms < perfect.rms, "line_4 ${line4.rms} should be quieter than perfect ${perfect.rms}")
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
        assertTrue(kotlin.math.abs(pcm.left.average()) < MAX_DC_OFFSET, "$label: left DC offset")
        assertTrue(kotlin.math.abs(pcm.right.average()) < MAX_DC_OFFSET, "$label: right DC offset")
    }

    private companion object {
        const val SAMPLE_RATE = 24_000
        const val MUSIC_FRAMES = 24_000
        const val SFX_FRAMES = 16_000
        const val MIN_AUDIBLE_RMS = 0.0001
        const val MAX_PEAK = 0.98f
        const val MAX_DC_OFFSET = 0.02
    }
}
