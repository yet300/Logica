package ge.yet.game.miniapp.audio.presets

import ge.yet.game.miniapp.audio.audioProgram
import ge.yet.game.miniapp.audio.MidiNote
import ge.yet.game.miniapp.audio.testing.AudioTestRenderResult
import ge.yet.game.miniapp.audio.testing.ExperimentalMiniAppAudioTestingApi
import ge.yet.game.miniapp.audio.testing.MiniAppAudioTestRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalMiniAppAudioTestingApi::class)
class InstrumentsTest {
    @Test
    fun `every shared instrument renders deterministic finite audible pcm`() {
        val cases = listOf(
            "soft_pad" to SoftPad(),
            "chip_lead" to ChipLead(),
            "analog_bass" to AnalogBass(),
            "glass_bell" to GlassBell(),
        )

        for ((name, fragment) in cases) {
            val program = audioProgram {
                tempo(240f)
                include(fragment)
                musicTrack("test") { instrument(name); notes(MidiNote.of(60)) }
            }
            val first = assertIs<AudioTestRenderResult.Success>(
                MiniAppAudioTestRenderer.render(program, 8_000, 8_000),
            ).pcm
            val second = assertIs<AudioTestRenderResult.Success>(
                MiniAppAudioTestRenderer.render(program, 8_000, 8_000),
            ).pcm

            assertEquals(first.quantizedPcmHash, second.quantizedPcmHash, name)
            assertTrue(first.rms > 0.001, name)
            assertTrue(first.peak < 1f, name)
            assertTrue(first.left.all(Float::isFinite) && first.right.all(Float::isFinite), name)
        }
    }

    @Test
    fun `shared instrument factories produce distinct bounded declarations`() {
        val program = audioProgram {
            include(SoftPad())
            include(ChipLead())
            include(AnalogBass())
            include(GlassBell())
        }

        assertEquals(
            listOf("soft_pad", "chip_lead", "analog_bass", "glass_bell"),
            program.instruments.map { it.name.value },
        )
        assertTrue(program.instruments.all { it.oscillators.isNotEmpty() })
        assertTrue(program.instruments.all { it.oscillators.size <= 8 && it.effects.size <= 4 })
        assertTrue(program.instruments.single { it.name.value == "glass_bell" }.partials.size >= 3)
    }

    @Test
    fun `instrument gain parameter changes only source gains`() {
        val quiet = audioProgram { include(SoftPad(gain = 0.2f)) }.instruments.single()
        val loud = audioProgram { include(SoftPad(gain = 0.8f)) }.instruments.single()

        assertTrue(quiet.oscillators.zip(loud.oscillators).all { (a, b) -> a.gain.value < b.gain.value })
        assertEquals(quiet.filters, loud.filters)
        assertEquals(quiet.envelope, loud.envelope)
    }
}
