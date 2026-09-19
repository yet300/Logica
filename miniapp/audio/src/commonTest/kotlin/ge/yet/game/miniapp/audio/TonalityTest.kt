package ge.yet.game.miniapp.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TonalityTest {
    @Test
    fun `scale modes expose their standard octave intervals`() {
        val expected = mapOf(
            ScaleMode.MAJOR to listOf(0, 2, 4, 5, 7, 9, 11),
            ScaleMode.NATURAL_MINOR to listOf(0, 2, 3, 5, 7, 8, 10),
            ScaleMode.DORIAN to listOf(0, 2, 3, 5, 7, 9, 10),
            ScaleMode.MIXOLYDIAN to listOf(0, 2, 4, 5, 7, 9, 10),
            ScaleMode.MAJOR_PENTATONIC to listOf(0, 2, 4, 7, 9),
            ScaleMode.MINOR_PENTATONIC to listOf(0, 3, 5, 7, 10),
        )

        expected.forEach { (mode, intervals) ->
            val scale = tonalScale(MidiNote.of(60), mode)
            assertEquals(intervals.map { 60 + it }, intervals.indices.map { scale.midi(it).value })
        }
    }

    @Test
    fun `degree arithmetic supports wrapping negative degrees and octave offsets`() {
        val major = tonalScale(MidiNote.of(60), ScaleMode.MAJOR)
        val minorPentatonic = tonalScale(MidiNote.of(57), ScaleMode.MINOR_PENTATONIC)

        assertEquals(72, major.midi(degree = 7).value)
        assertEquals(59, major.midi(degree = -1).value)
        assertEquals(72, major.midi(degree = 0, octave = 1).value)
        assertEquals(55, minorPentatonic.midi(degree = -1).value)
        assertEquals(69, minorPentatonic.midi(degree = 5).value)
    }

    @Test
    fun `note forwards velocity and midi range remains bounded`() {
        val scale = tonalScale(MidiNote.of(60), ScaleMode.DORIAN)

        assertEquals(AudioNote.Pitched(MidiNote.of(63), 0.42f), scale.note(degree = 2, velocity = 0.42f))
        assertFailsWith<IllegalArgumentException> {
            tonalScale(MidiNote.of(0), ScaleMode.MAJOR).midi(-1)
        }
        assertFailsWith<IllegalArgumentException> {
            tonalScale(MidiNote.of(127), ScaleMode.MAJOR).midi(1)
        }
    }
}
