package ge.yet.game.miniapp.audio.presets

import ge.yet.game.miniapp.audio.audioProgram
import ge.yet.game.miniapp.audio.SfxName
import ge.yet.game.miniapp.audio.testing.AudioTestRenderRequest
import ge.yet.game.miniapp.audio.testing.AudioTestRenderResult
import ge.yet.game.miniapp.audio.testing.AudioTestSfxTrigger
import ge.yet.game.miniapp.audio.testing.ExperimentalMiniAppAudioTestingApi
import ge.yet.game.miniapp.audio.testing.MiniAppAudioTestRenderer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalMiniAppAudioTestingApi::class)
class SoundEffectsTest {
    @Test
    fun `wooden placement thock is deterministic and bounded`() {
        val program = audioProgram { include(WoodenPlacementThock(name = "lock")) }
        val request = AudioTestRenderRequest(
            sampleRate = 8_000,
            frameCount = 4_000,
            includeMusic = false,
            sfxTriggers = listOf(AudioTestSfxTrigger(SfxName("lock"), 0)),
        )

        val first = assertIs<AudioTestRenderResult.Success>(
            MiniAppAudioTestRenderer.render(program, request),
        ).pcm
        val second = assertIs<AudioTestRenderResult.Success>(
            MiniAppAudioTestRenderer.render(program, request),
        ).pcm

        assertEquals(first.quantizedPcmHash, second.quantizedPcmHash)
        assertContentEquals(first.left, second.left)
        assertContentEquals(first.right, second.right)
        assertTrue(first.rms > 0.0005)
        assertTrue(first.peak < 1f)
    }

    @Test
    fun `every shared sfx renders deterministic finite audible pcm with headroom`() {
        val fragments = listOf(
            PlacementClick(),
            WoodenPlacementThock(),
            SuccessSweep(),
            Explosion(seed = 42),
            PowerUp(),
        )

        for (fragment in fragments) {
            val program = audioProgram { include(fragment) }
            val name = program.soundEffects.single().name
            val request = AudioTestRenderRequest(
                sampleRate = 8_000,
                frameCount = 4_000,
                includeMusic = false,
                sfxTriggers = listOf(AudioTestSfxTrigger(SfxName(name.value), 0)),
            )
            val first = assertIs<AudioTestRenderResult.Success>(MiniAppAudioTestRenderer.render(program, request)).pcm
            val second = assertIs<AudioTestRenderResult.Success>(MiniAppAudioTestRenderer.render(program, request)).pcm

            assertEquals(first.quantizedPcmHash, second.quantizedPcmHash, name.value)
            assertTrue(first.rms > 0.0005, name.value)
            assertTrue(first.peak < 1f, name.value)
            assertTrue(first.left.all(Float::isFinite) && first.right.all(Float::isFinite), name.value)
        }
    }

    @Test
    fun `shared sfx factories expose original bounded declarations`() {
        val program = audioProgram {
            include(PlacementClick())
            include(WoodenPlacementThock())
            include(SuccessSweep())
            include(Explosion(seed = 42))
            include(PowerUp())
        }

        assertEquals(
            listOf("placement_click", "wooden_placement_thock", "success_sweep", "explosion", "power_up"),
            program.soundEffects.map { it.name.value },
        )
        assertTrue(program.soundEffects.all { it.oscillators.isNotEmpty() || it.noises.isNotEmpty() })
        assertTrue(program.soundEffects.all { it.effects.size <= 4 })
        assertTrue(program.soundEffects.all { it.envelope != null })
        assertTrue(program.soundEffects.all { it.pitch != null })
        assertEquals(42, program.soundEffects.single { it.name.value == "explosion" }.noises.first().seed)
    }

    @Test
    fun `sfx names and gain are configurable without changing synthesis shape`() {
        val quiet = audioProgram { include(PlacementClick(name = "quiet_click", gain = 0.2f)) }
            .soundEffects.single()
        val loud = audioProgram { include(PlacementClick(name = "loud_click", gain = 0.8f)) }
            .soundEffects.single()

        assertEquals("quiet_click", quiet.name.value)
        assertEquals("loud_click", loud.name.value)
        assertTrue(quiet.oscillators.first().gain.value < loud.oscillators.first().gain.value)
        assertEquals(quiet.pitch, loud.pitch)
        assertNotNull(quiet.envelope)
    }
}
