package ge.yet.game.miniapp.audio.presets

import ge.yet.game.miniapp.audio.AudioCommandResult
import ge.yet.game.miniapp.audio.AudioControlName
import ge.yet.game.miniapp.audio.AudioDuration
import ge.yet.game.miniapp.audio.AudioProgram
import ge.yet.game.miniapp.audio.MiniAppAudio
import ge.yet.game.miniapp.audio.SfxName
import ge.yet.game.miniapp.audio.audioProgram
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SessionAudioProgramTest {
    @Test
    fun `start plays music exactly once per session`() {
        val audio = RecordingAudio()
        val player = SessionAudioProgram(audio, testProgram)

        player.start()
        player.start()
        player.start()

        assertEquals(1, audio.musicStarts)
        assertSame(testProgram, audio.lastMusicProgram)
    }

    @Test
    fun `sfx routes through the bound program`() {
        val audio = RecordingAudio()
        val player = SessionAudioProgram(audio, testProgram)

        player.playSfx(SfxName("placement_click"))

        assertEquals(listOf("placement_click"), audio.sfxNames)
        assertEquals(listOf(testProgram), audio.sfxPrograms)
    }

    private val testProgram: AudioProgram = audioProgram {
        include(PlacementClick())
    }

    private class RecordingAudio : MiniAppAudio {
        var musicStarts = 0
        var lastMusicProgram: AudioProgram? = null
        val sfxPrograms = mutableListOf<AudioProgram>()
        val sfxNames = mutableListOf<String>()

        override fun playMusic(program: AudioProgram): AudioCommandResult {
            musicStarts += 1
            lastMusicProgram = program
            return AudioCommandResult.Accepted
        }

        override fun stopMusic(fadeOut: AudioDuration): AudioCommandResult =
            AudioCommandResult.Accepted

        override fun playSfx(program: AudioProgram, name: SfxName): AudioCommandResult {
            sfxPrograms += program
            sfxNames += name.value
            return AudioCommandResult.Accepted
        }

        override fun setControl(name: AudioControlName, value: Float): AudioCommandResult =
            AudioCommandResult.Accepted
    }
}
