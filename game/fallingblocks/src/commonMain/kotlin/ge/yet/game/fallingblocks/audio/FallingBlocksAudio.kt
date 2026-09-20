package ge.yet.game.fallingblocks.audio

import ge.yet.game.miniapp.audio.AudioControlName
import ge.yet.game.miniapp.audio.SfxName

internal object FallingBlocksAudio {
    val Intensity = AudioControlName("intensity")

    val Move = SfxName("move")
    val Rotate = SfxName("rotate")
    val SoftDrop = SfxName("soft_drop")
    val HardDrop = SfxName("hard_drop")
    val Lock = SfxName("lock")
    val Line1 = SfxName("line_1")
    val Line2 = SfxName("line_2")
    val Line3 = SfxName("line_3")
    val Line4 = SfxName("line_4")
    val Perfect = SfxName("perfect")
    val LevelUp = SfxName("level_up")
    val GameOver = SfxName("game_over")
    val Revive = SfxName("revive")

    val allSfx: List<SfxName> = listOf(
        Move, Rotate, SoftDrop, HardDrop, Lock,
        Line1, Line2, Line3, Line4, Perfect,
        LevelUp, GameOver, Revive,
    )
}
