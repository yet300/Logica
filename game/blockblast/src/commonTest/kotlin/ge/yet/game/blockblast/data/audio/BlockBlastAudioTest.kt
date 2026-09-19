package ge.yet.game.blockblast.data.audio

import ge.yet.game.blockblast.audio.BlockBlastAudio
import ge.yet.game.blockblast.domain.model.FeedbackType
import ge.yet.game.miniapp.audio.AudioCommandResult
import ge.yet.game.miniapp.audio.AudioControlName
import ge.yet.game.miniapp.audio.AudioDuration
import ge.yet.game.miniapp.audio.AudioProgram
import ge.yet.game.miniapp.audio.MiniAppAudio
import ge.yet.game.miniapp.audio.SfxName
import ge.yet.game.miniapp.audio.testing.AudioTestRenderResult
import ge.yet.game.miniapp.audio.testing.ExperimentalMiniAppAudioTestingApi
import ge.yet.game.miniapp.audio.testing.MiniAppAudioTestRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalMiniAppAudioTestingApi::class)
class BlockBlastAudioTest {
    @Test
    fun grove_marimba_is_original_bounded_and_deterministic() {
        assertEquals(listOf("grove_marimba"), BlockBlastAudio.program.instruments.map { it.name.value })
        assertEquals(listOf("grove_marimba"), BlockBlastAudio.program.musicTracks.map { it.name.value })
        assertEquals(listOf(4, 2, 2), BlockBlastAudio.program.musicTracks.single().sections.map { it.cycles })
        assertEquals(null, BlockBlastAudio.program.musicTracks.single().sections.last().pattern)

        val frameCount = 8_000 * 240 * 8 / 104
        val first = assertIs<AudioTestRenderResult.Success>(
            MiniAppAudioTestRenderer.render(BlockBlastAudio.program, 8_000, frameCount),
        ).pcm
        val second = assertIs<AudioTestRenderResult.Success>(
            MiniAppAudioTestRenderer.render(BlockBlastAudio.program, 8_000, frameCount),
        ).pcm

        assertEquals(first.quantizedPcmHash, second.quantizedPcmHash)
        assertTrue(first.rms > 0.005)
        assertTrue(first.peak <= 0.9101f)
        assertTrue(first.left.all(Float::isFinite) && first.right.all(Float::isFinite))
    }

    @Test
    fun start_is_idempotent_for_the_session_even_when_host_rejects_it() {
        val audio = RecordingMiniAppAudio(playMusicResult = AudioCommandResult.Rejected(
            ge.yet.game.miniapp.audio.AudioCommandRejection.PLAYBACK_SUPPRESSED,
        ))
        val player = ProceduralBlockBlastAudioPlayer(audio)

        player.start()
        player.start()

        assertEquals(listOf<Command>(Command.PlayMusic(BlockBlastAudio.program)), audio.commands)
    }
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
        data class PlayMusic(val program: AudioProgram) : Command
        data class PlaySfx(val program: AudioProgram, val name: SfxName) : Command
    }

    private class RecordingMiniAppAudio(
        private val playMusicResult: AudioCommandResult = AudioCommandResult.Accepted,
    ) : MiniAppAudio {
        val commands = mutableListOf<Command>()

        override fun playMusic(program: AudioProgram): AudioCommandResult =
            playMusicResult.also { commands += Command.PlayMusic(program) }

        override fun stopMusic(fadeOut: AudioDuration): AudioCommandResult =
            AudioCommandResult.Accepted

        override fun playSfx(program: AudioProgram, name: SfxName): AudioCommandResult =
            AudioCommandResult.Accepted.also { commands += Command.PlaySfx(program, name) }

        override fun setControl(name: AudioControlName, value: Float): AudioCommandResult =
            AudioCommandResult.Accepted
    }
}
