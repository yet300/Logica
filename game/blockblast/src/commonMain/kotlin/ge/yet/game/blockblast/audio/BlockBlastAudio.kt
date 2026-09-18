package ge.yet.game.blockblast.audio

import ge.yet.game.blockblast.domain.model.FeedbackType
import ge.yet.game.miniapp.audio.SfxName
import ge.yet.game.miniapp.audio.audioProgram
import ge.yet.game.miniapp.audio.presets.PowerUp
import ge.yet.game.miniapp.audio.presets.SuccessSweep

/**
 * SFX-only program: Block Blast ships no music, only sound effects.
 * Music tracks, instruments and adaptive controls were removed; the program
 * is a stable named scope routing the game-owned voices plus two reused
 * preset stingers through one [MiniAppAudio] handle.
 */
internal object BlockBlastAudio {
    val Place = SfxName("place")
    val ClearPop = SfxName("clear_pop")
    val GameOver = SfxName("game_over")
    val Revive = SfxName("revive")
    val NewBest = SfxName("new_best")

    val VoiceGood = SfxName("voice_good")
    val VoiceGreat = SfxName("voice_great")
    val VoiceAmazing = SfxName("voice_amazing")
    val VoiceExcellent = SfxName("voice_excellent")
    val VoiceUnbelievable = SfxName("voice_unbelievable")

    fun voice(type: FeedbackType): SfxName = when (type) {
        FeedbackType.GOOD -> VoiceGood
        FeedbackType.GREAT -> VoiceGreat
        FeedbackType.AMAZING -> VoiceAmazing
        FeedbackType.EXCELLENT -> VoiceExcellent
        FeedbackType.UNBELIEVABLE -> VoiceUnbelievable
    }

    val program = audioProgram {
        include(PowerUp(name = Revive.value, gain = 0.26f))
        include(SuccessSweep(name = NewBest.value, gain = 0.30f))
        include(BlockBlastSfx.fragment)
    }
}
