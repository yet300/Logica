package ge.yet.game.fallingblocks.audio

import ge.yet.game.fallingblocks.gameFixture
import ge.yet.game.fallingblocks.domain.model.GameAction
import ge.yet.game.fallingblocks.domain.model.GameFact
import ge.yet.game.miniapp.audio.AudioCommandRejection
import ge.yet.game.miniapp.audio.AudioCommandResult
import ge.yet.game.miniapp.audio.AudioControlName
import ge.yet.game.miniapp.audio.AudioDuration
import ge.yet.game.miniapp.audio.AudioProgram
import ge.yet.game.miniapp.audio.MiniAppAudio
import ge.yet.game.miniapp.audio.SfxName
import kotlin.test.Test
import kotlin.test.assertEquals

class FallingBlocksAudioAdapterTest {
    @Test
    fun `legal player actions and engine facts map to exact ordered SFX`() {
        val audio = RecordingAudio()
        val adapter = FallingBlocksAudioAdapter(audio)
        val state = gameFixture()

        adapter.start(state)
        adapter.onTransition(GameAction.RotateClockwise, state, listOf(GameFact.Rotated))
        adapter.onTransition(GameAction.MoveHorizontal(1), state, listOf(GameFact.Moved(1, 0)))
        adapter.onTransition(GameAction.SoftDrop(1), state, listOf(GameFact.Moved(0, 1)))
        adapter.onTransition(
            GameAction.HardDrop,
            state.copy(level = 5),
            listOf(
                GameFact.HardDropped(8),
                GameFact.Locked,
                GameFact.LinesCleared(count = 4, perfect = true),
                GameFact.LevelChanged(level = 5),
                GameFact.ToppedOut,
            ),
        )
        adapter.onTransition(GameAction.Revive, state.copy(level = 5), listOf(GameFact.Revived))

        assertEquals(1, audio.musicCalls)
        assertEquals(
            listOf(
                "rotate",
                "move",
                "soft_drop",
                "hard_drop",
                "lock",
                "perfect",
                "level_up",
                "game_over",
                "revive",
            ),
            audio.sfxNames,
        )
        assertEquals(
            listOf(FallingBlocksAudio.Intensity to 0.18f, FallingBlocksAudio.Intensity to 0.42f),
            audio.controls,
        )
    }

    @Test
    fun `blocked and gravity movement stay silent`() {
        val audio = RecordingAudio()
        val adapter = FallingBlocksAudioAdapter(audio)
        val state = gameFixture()

        adapter.start(state)
        adapter.onTransition(GameAction.RotateClockwise, state, listOf(GameFact.Blocked))
        adapter.onTransition(GameAction.MoveHorizontal(1), state, listOf(GameFact.Blocked))
        adapter.onTransition(GameAction.SoftDrop(1), state, listOf(GameFact.Blocked))
        adapter.onTransition(GameAction.AdvanceTime(800), state, listOf(GameFact.Moved(0, 1)))

        assertEquals(emptyList(), audio.sfxNames)
    }

    @Test
    fun `intensity updates only when its discrete band changes`() {
        val audio = RecordingAudio()
        val adapter = FallingBlocksAudioAdapter(audio)

        adapter.start(gameFixture(level = 1))
        adapter.start(gameFixture(level = 4))
        adapter.start(gameFixture(level = 5))
        adapter.start(gameFixture(level = 8))
        adapter.start(gameFixture(level = 9))
        adapter.start(gameFixture(level = 13))

        assertEquals(
            listOf(0.18f, 0.42f, 0.68f, 0.90f),
            audio.controls.map { it.second },
        )
    }

    @Test
    fun `host rejections are consumed once without retries`() {
        val audio = RecordingAudio(
            result = AudioCommandResult.Rejected(AudioCommandRejection.PLAYBACK_SUPPRESSED),
        )
        val adapter = FallingBlocksAudioAdapter(audio)
        val state = gameFixture()

        adapter.start(state)
        adapter.start(state)
        adapter.onTransition(GameAction.RotateClockwise, state, listOf(GameFact.Rotated))

        assertEquals(1, audio.musicCalls)
        assertEquals(listOf("rotate"), audio.sfxNames)
        assertEquals(listOf(FallingBlocksAudio.Intensity to 0.18f), audio.controls)
    }

    private class RecordingAudio(
        private val result: AudioCommandResult = AudioCommandResult.Accepted,
    ) : MiniAppAudio {
        var musicCalls = 0
        val sfxNames = mutableListOf<String>()
        val controls = mutableListOf<Pair<AudioControlName, Float>>()

        override fun playMusic(program: AudioProgram): AudioCommandResult {
            musicCalls += 1
            return result
        }

        override fun stopMusic(fadeOut: AudioDuration): AudioCommandResult = result

        override fun playSfx(program: AudioProgram, name: SfxName): AudioCommandResult {
            sfxNames += name.value
            return result
        }

        override fun setControl(name: AudioControlName, value: Float): AudioCommandResult {
            controls += name to value
            return result
        }
    }
}
