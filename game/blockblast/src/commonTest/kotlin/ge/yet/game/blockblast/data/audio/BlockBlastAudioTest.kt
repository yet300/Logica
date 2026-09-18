package ge.yet.game.blockblast.data.audio

import ge.yet.game.blockblast.audio.BlockBlastAudio
import ge.yet.game.blockblast.domain.model.FeedbackType
import ge.yet.game.miniapp.audio.AudioCommandResult
import ge.yet.game.miniapp.audio.AudioControlName
import ge.yet.game.miniapp.audio.AudioDuration
import ge.yet.game.miniapp.audio.AudioProgram
import ge.yet.game.miniapp.audio.MiniAppAudio
import ge.yet.game.miniapp.audio.SfxName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlockBlastAudioTest {
    @Test
    fun every_feedback_type_maps_to_its_own_voice() {
        val audio = RecordingMiniAppAudio()
        val player = ProceduralBlockBlastAudioPlayer(audio)

        player.playFeedback(FeedbackType.GOOD)
        player.playFeedback(FeedbackType.GREAT)
        player.playFeedback(FeedbackType.AMAZING)
        player.playFeedback(FeedbackType.EXCELLENT)
        player.playFeedback(FeedbackType.UNBELIEVABLE)

        assertEquals(
            listOf<Command>(
                Command.PlaySfx(BlockBlastAudio.program, BlockBlastAudio.VoiceGood),
                Command.PlaySfx(BlockBlastAudio.program, BlockBlastAudio.VoiceGreat),
                Command.PlaySfx(BlockBlastAudio.program, BlockBlastAudio.VoiceAmazing),
                Command.PlaySfx(BlockBlastAudio.program, BlockBlastAudio.VoiceExcellent),
                Command.PlaySfx(BlockBlastAudio.program, BlockBlastAudio.VoiceUnbelievable),
            ),
            audio.commands,
        )
    }

    @Test
    fun event_roles_map_to_their_own_sfx() {
        val audio = RecordingMiniAppAudio()
        val player = ProceduralBlockBlastAudioPlayer(audio)

        player.playPlace()
        player.playClear(lines = 1)
        player.playClear(lines = 3)
        player.playGameOver()
        player.playRevive()
        player.playNewBest()

        assertEquals(
            listOf<Command>(
                Command.PlaySfx(BlockBlastAudio.program, BlockBlastAudio.Place),
                Command.PlaySfx(BlockBlastAudio.program, BlockBlastAudio.ClearPop),
                Command.PlaySfx(BlockBlastAudio.program, BlockBlastAudio.ClearPop),
                Command.PlaySfx(BlockBlastAudio.program, BlockBlastAudio.GameOver),
                Command.PlaySfx(BlockBlastAudio.program, BlockBlastAudio.Revive),
                Command.PlaySfx(BlockBlastAudio.program, BlockBlastAudio.NewBest),
            ),
            audio.commands,
        )
    }

    @Test
    fun clear_with_no_lines_stays_silent() {
        val audio = RecordingMiniAppAudio()
        val player = ProceduralBlockBlastAudioPlayer(audio)

        player.playClear(lines = 0)

        assertTrue(audio.commands.isEmpty())
    }

    private sealed interface Command {
        data class PlaySfx(val program: AudioProgram, val name: SfxName) : Command
    }

    private class RecordingMiniAppAudio : MiniAppAudio {
        val commands = mutableListOf<Command>()

        override fun playMusic(program: AudioProgram): AudioCommandResult =
            AudioCommandResult.Accepted

        override fun stopMusic(fadeOut: AudioDuration): AudioCommandResult =
            AudioCommandResult.Accepted

        override fun playSfx(program: AudioProgram, name: SfxName): AudioCommandResult =
            AudioCommandResult.Accepted.also { commands += Command.PlaySfx(program, name) }

        override fun setControl(name: AudioControlName, value: Float): AudioCommandResult =
            AudioCommandResult.Accepted
    }
}
