package ge.yet.game.blockblast.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlockBlastAudioProgramTest {
    @Test
    fun `program preserves approved sfx and exposes only grove marimba music`() {
        val program = BlockBlastAudio.program

        assertTrue(program.controls.isEmpty())
        assertEquals(listOf("grove_marimba"), program.musicTracks.map { it.name.value })
        assertEquals(listOf("grove_marimba"), program.instruments.map { it.name.value })
        assertEquals(
            setOf(
                "place",
                "clear_pop",
                "game_over",
                "revive",
                "new_best",
                "voice_good",
                "voice_great",
                "voice_amazing",
                "voice_excellent",
                "voice_unbelievable",
            ),
            program.soundEffects.mapTo(linkedSetOf()) { it.name.value },
        )
        assertEquals(10, program.soundEffects.map { it.name }.distinct().size)
        assertEquals(
            setOf(
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
            ),
            program.soundEffects.mapTo(linkedSetOf()) { it.name },
        )
    }

    @Test
    fun `program keeps the approved deterministic noise seeds`() {
        val program = BlockBlastAudio.program

        assertEquals(
            mapOf(
                "place" to listOf(8_048_402L),
                "clear_pop" to listOf(8_048_401L),
            ),
            program.soundEffects
                .filter { it.noises.isNotEmpty() }
                .associate { effect -> effect.name.value to effect.noises.map { it.seed } },
        )
    }
}
