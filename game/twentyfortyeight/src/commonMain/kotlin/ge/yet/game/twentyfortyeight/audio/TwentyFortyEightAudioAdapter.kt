package ge.yet.game.twentyfortyeight.audio

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import ge.yet.game.miniapp.audio.MiniAppAudio
import ge.yet.game.miniapp.audio.SfxName
import ge.yet.game.miniapp.audio.consumeSilently
import ge.yet.game.miniapp.metro.MiniAppSessionScope
import ge.yet.game.twentyfortyeight.domain.engine.AudioControls
import ge.yet.game.twentyfortyeight.domain.model.TileValue

internal sealed interface AudioEvent {
    data object TileSpawn : AudioEvent
    data object Move : AudioEvent

    data class MoveResolved(
        val spawned: Boolean,
        val mergeValues: List<TileValue>,
    ) : AudioEvent

    data object Undo : AudioEvent
    data object NewBest : AudioEvent
    data object Victory : AudioEvent
    data object GameOver : AudioEvent
}

@SingleIn(MiniAppSessionScope::class)
internal class TwentyFortyEightAudioAdapter @Inject constructor(
    private val audio: MiniAppAudio,
) {
    private var started = false
    private var lastAttemptedControls: AudioControls? = null

    fun start() {
        if (started) return
        started = true
        audio.playMusic(TwentyFortyEightAudio.program).consumeSilently()
    }

    fun updateControls(controls: AudioControls) {
        start()
        if (controls == lastAttemptedControls) return
        lastAttemptedControls = controls

        audio.setControl(TwentyFortyEightAudio.Progress, controls.progress).consumeSilently()
        audio.setControl(TwentyFortyEightAudio.Danger, controls.danger).consumeSilently()
        audio.setControl(TwentyFortyEightAudio.Momentum, controls.momentum).consumeSilently()
    }

    fun play(event: AudioEvent) {
        when (event) {
            AudioEvent.TileSpawn -> playSfx(TwentyFortyEightAudio.TileSpawn)
            AudioEvent.Move -> playSfx(TwentyFortyEightAudio.Move)
            is AudioEvent.MoveResolved -> playMove(event)
            AudioEvent.Undo -> playSfx(TwentyFortyEightAudio.Undo)
            AudioEvent.NewBest -> playSfx(TwentyFortyEightAudio.NewBest)
            AudioEvent.Victory -> playSfx(TwentyFortyEightAudio.Victory)
            AudioEvent.GameOver -> playSfx(TwentyFortyEightAudio.GameOver)
        }
    }

    private fun playMove(event: AudioEvent.MoveResolved) {
        playSfx(TwentyFortyEightAudio.Move)
        if (event.spawned) playSfx(TwentyFortyEightAudio.TileSpawn)

        val mergeSfx = when (event.mergeValues.maxOfOrNull(TileValue::value)) {
            in 4L..32L -> TwentyFortyEightAudio.MergeLow
            in 64L..512L -> TwentyFortyEightAudio.MergeMid
            in 1_024L..TileValue.MAX_VALUE -> TwentyFortyEightAudio.MergeHigh
            else -> null
        }
        if (mergeSfx != null) playSfx(mergeSfx)
    }

    private fun playSfx(name: SfxName) {
        audio.playSfx(TwentyFortyEightAudio.program, name).consumeSilently()
    }
}
