package ge.yet.game.miniapp.audio

import ge.yet.game.pattern.PatternQueryBudget
import ge.yet.game.pattern.TimeArc
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class AudioProgramDslTest {
    @Test
    fun `pitched note velocity is finite and normalized`() {
        assertEquals(0.37f, AudioNote.Pitched(MidiNote.of(60), velocity = 0.37f).velocity)
        assertFailsWith<IllegalArgumentException> { AudioNote.Pitched(MidiNote.of(60), velocity = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { AudioNote.Pitched(MidiNote.of(60), velocity = -0.01f) }
        assertFailsWith<IllegalArgumentException> { AudioNote.Pitched(MidiNote.of(60), velocity = 1.01f) }
    }

    @Test
    fun `humanized notes snapshot deterministic seeded velocities`() {
        val notes = listOf(MidiNote.of(60), MidiNote.of(63), MidiNote.of(67), MidiNote.of(70))

        val first = humanizedNotes(notes, velocity = 0.7f..0.95f, seed = 7L)
            .query(TimeArc.unit, PatternQueryBudget())
        val repeated = humanizedNotes(notes, velocity = 0.7f..0.95f, seed = 7L)
            .query(TimeArc.unit, PatternQueryBudget())
        val changed = humanizedNotes(notes, velocity = 0.7f..0.95f, seed = 8L)
            .query(TimeArc.unit, PatternQueryBudget())

        assertEquals(first, repeated)
        assertNotEquals(
            first.map { (it.value as AudioNote.Pitched).velocity },
            changed.map { (it.value as AudioNote.Pitched).velocity },
        )
    }

    @Test
    fun `note frequency parameter exposes a positive bounded midi range`() {
        val parameter = assertIs<AudioParameter.NoteFrequency>(
            noteFrequency(ratio = 2.2f, offsetHz = 80f),
        )

        assertEquals(2.2f, parameter.ratio)
        assertEquals(80f, parameter.offsetHz)
        kotlin.test.assertTrue(parameter.outputRange.start > 0f)
        kotlin.test.assertTrue(parameter.outputRange.endInclusive > parameter.outputRange.start)
        assertFailsWith<IllegalArgumentException> { noteFrequency(ratio = 0f) }
        assertFailsWith<IllegalArgumentException> { noteFrequency(ratio = 1f, offsetHz = -100f) }
    }

    @Test
    fun `partial snapshots its own envelope`() {
        val partial = audioProgram {
            instrument("mallet") {
                oscillator(OscillatorShape.SINE)
                partial(ratio = 3.7f, gain = 0.12f) {
                    envelope(attack = 0.ms, decay = 95.ms, sustain = 0f, release = 20.ms)
                }
            }
        }.instruments.single().partials.single()

        assertEquals(3.7f, partial.ratio)
        assertEquals(0.12f, partial.gain.value)
        assertEquals(95.ms, requireNotNull(partial.envelope).decay)
    }

    @Test
    fun `arrangement snapshots bounded sections`() {
        val phrase = ge.yet.game.pattern.sequence(listOf(AudioNote.Pitched(MidiNote.of(60))))
        val track = audioProgram {
            instrument("lead") { oscillator(OscillatorShape.SINE) }
            musicTrack("line") {
                instrument("lead")
                arrangement {
                    section(cycles = 4, notes = phrase)
                    section(cycles = 2, notes = phrase, transposeSemitones = 5)
                    section(cycles = 2, muted = true)
                }
            }
        }.musicTracks.single()

        assertEquals(listOf(4, 2, 2), track.sections.map { it.cycles })
        assertEquals(listOf(0, 5, 0), track.sections.map { it.transposeSemitones })
        assertEquals(null, track.sections.last().pattern)
    }

    @Test
    fun `music bus snapshots compressor and limiter`() {
        val bus = audioProgram {
            musicBus {
                reverb(0.18f)
                compressor(0.68f, 3f, 8.ms, 110.ms, 1.05f)
                limiter(0.92f, 70.ms)
            }
        }.musicBus

        assertIs<BusEffectDeclaration.Reverb>(bus.effects[0])
        assertIs<BusEffectDeclaration.Compressor>(bus.effects[1])
        assertIs<BusEffectDeclaration.Limiter>(bus.effects[2])
    }




    @Test
    fun `builder snapshots controls instruments tracks and sfx`() {
        val program = audioProgram {
            tempo(112f)
            control("intensity", default = 0.25f, range = 0f..1f)
            instrument("bass") {
                oscillator(OscillatorShape.SAW, gain = 0.8f, detuneCents = -4f)
                envelope(attack = 8.ms, decay = 140.ms, sustain = 0.45f, release = 180.ms)
            }
            musicTrack("bassline") {
                instrument("bass")
                notes(MidiNote.of(36), MidiNote.of(36), MidiNote.of(39))
            }
            sfx("place") {
                oscillator(OscillatorShape.SINE)
                pitch(from = 240.hz, to = 120.hz, duration = 55.ms)
                envelope(attack = 1.ms, release = 60.ms)
            }
        }

        assertEquals(Tempo.of(112f), program.tempo)
        assertEquals(listOf(AudioControlName("intensity")), program.controls.map { it.name })
        assertEquals(listOf(InstrumentName("bass")), program.instruments.map { it.name })
        assertEquals(listOf(MusicTrackName("bassline")), program.musicTracks.map { it.name })
        assertEquals(listOf(SfxName("place")), program.soundEffects.map { it.name })
        assertEquals(
            3,
            program.musicTracks.single().pattern.query(TimeArc.unit, PatternQueryBudget()).size,
        )
    }

    @Test
    fun `built program is isolated from mutable author inputs`() {
        val notes = mutableListOf(MidiNote.of(36))
        val program = audioProgram {
            instrument("bass") { oscillator(OscillatorShape.SINE) }
            musicTrack("line") {
                instrument("bass")
                notes(notes)
            }
        }

        notes += MidiNote.of(48)

        assertEquals(
            listOf(AudioNote.Pitched(MidiNote.of(36))),
            program.musicTracks.single().pattern.query(TimeArc.unit, PatternQueryBudget()).map { it.value },
        )
    }

    @Test
    fun `builder rejects duplicate declaration names immediately`() {
        assertFailsWith<IllegalArgumentException> {
            audioProgram {
                control("intensity", 0.2f, 0f..1f)
                control("intensity", 0.3f, 0f..1f)
            }
        }
        assertFailsWith<IllegalArgumentException> {
            audioProgram {
                instrument("lead") { oscillator(OscillatorShape.SINE) }
                instrument("lead") { oscillator(OscillatorShape.SAW) }
            }
        }
    }

    @Test
    fun `program lookup returns typed found and missing results`() {
        val program = audioProgram {
            control("intensity", 0.4f, 0f..1f)
            sfx("place") { oscillator(OscillatorShape.SINE) }
        }

        val found = assertIs<AudioLookupResult.Found<SoundEffectDeclaration>>(program.sfx(SfxName("place")))
        val missing = assertIs<AudioLookupResult.Missing>(program.sfx(SfxName("missing")))

        assertEquals(SfxName("place"), found.value.name)
        assertEquals("sfx[missing]", missing.path)
        assertIs<AudioLookupResult.Found<AudioControlDeclaration>>(program.control(AudioControlName("intensity")))
    }
}
