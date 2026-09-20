package ge.yet.game.fallingblocks.audio

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.domain.model.GameAction
import ge.yet.game.fallingblocks.domain.model.GameFact
import ge.yet.game.miniapp.audio.MiniAppAudio
import ge.yet.game.miniapp.audio.SfxName
import ge.yet.game.miniapp.audio.consumeSilently
import ge.yet.game.miniapp.audio.presets.SessionAudioProgram
import ge.yet.game.miniapp.metro.MiniAppSessionScope

internal interface FallingBlocksAudioPlayer {
    fun start(state: FallingBlocksState)
    fun onTransition(action: GameAction, state: FallingBlocksState, facts: List<GameFact>)
}

@SingleIn(MiniAppSessionScope::class)
internal class FallingBlocksAudioAdapter @Inject constructor(
    audio: MiniAppAudio,
) : FallingBlocksAudioPlayer {
    private val session = SessionAudioProgram(audio, FallingBlocksProgram)
    private val facade = audio
    private var lastIntensityBand: Int? = null

    override fun start(state: FallingBlocksState) {
        session.start()
        updateIntensity(state)
    }

    override fun onTransition(action: GameAction, state: FallingBlocksState, facts: List<GameFact>) {
        start(state)
        when (action) {
            GameAction.RotateClockwise -> if (GameFact.Rotated in facts) play(FallingBlocksAudio.Rotate)
            is GameAction.MoveHorizontal -> if (facts.any { it is GameFact.Moved && it.horizontalCells != 0 }) {
                play(FallingBlocksAudio.Move)
            }
            is GameAction.SoftDrop -> if (facts.any { it is GameFact.Moved && it.downwardCells > 0 }) {
                play(FallingBlocksAudio.SoftDrop)
            }
            GameAction.HardDrop -> if (facts.any { it is GameFact.HardDropped }) play(FallingBlocksAudio.HardDrop)
            is GameAction.AdvanceTime -> Unit
            GameAction.Revive -> if (GameFact.Revived in facts) play(FallingBlocksAudio.Revive)
        }
        if (GameFact.Locked in facts) play(FallingBlocksAudio.Lock)
        facts.filterIsInstance<GameFact.LinesCleared>().forEach { clear ->
            play(
                if (clear.perfect) FallingBlocksAudio.Perfect else when (clear.count) {
                    1 -> FallingBlocksAudio.Line1
                    2 -> FallingBlocksAudio.Line2
                    3 -> FallingBlocksAudio.Line3
                    else -> FallingBlocksAudio.Line4
                },
            )
        }
        if (facts.any { it is GameFact.LevelChanged }) play(FallingBlocksAudio.LevelUp)
        if (GameFact.ToppedOut in facts) play(FallingBlocksAudio.GameOver)
        updateIntensity(state)
    }

    private fun updateIntensity(state: FallingBlocksState) {
        val highest = state.board.cells.indexOfFirst { it != null }.let { index ->
            if (index < 0) Board.TOTAL_HEIGHT else index / Board.WIDTH
        }
        val occupiedRows = (Board.TOTAL_HEIGHT - highest).coerceAtLeast(0)
        val stackBand = when {
            occupiedRows >= 16 -> 3
            occupiedRows >= 11 -> 2
            occupiedRows >= 6 -> 1
            else -> 0
        }
        val levelBand = ((state.level - 1) / 4).coerceIn(0, 3)
        val band = maxOf(stackBand, levelBand)
        if (band == lastIntensityBand) return
        lastIntensityBand = band
        val value = listOf(0.18f, 0.42f, 0.68f, 0.90f)[band]
        facade.setControl(FallingBlocksAudio.Intensity, value).consumeSilently()
    }

    private fun play(name: SfxName) = session.playSfx(name)
}
