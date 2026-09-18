package ge.yet.game.blockblast.data.audio

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import ge.yet.game.blockblast.audio.BlockBlastAudio
import ge.yet.game.blockblast.domain.model.FeedbackType
import ge.yet.game.miniapp.audio.MiniAppAudio
import ge.yet.game.miniapp.audio.presets.SessionAudioProgram
import ge.yet.game.miniapp.metro.MiniAppSessionScope

internal interface BlockBlastAudioPlayer {
    fun playFeedback(type: FeedbackType)
    fun playPlace()
    fun playClear(lines: Int)
    fun playGameOver()
    fun playRevive()
    fun playNewBest()
}

// Session-scoped (not by state — stateless — but by the graph identity test);
// scope lives here because Metro forbids scopes on @Binds declarations.
@SingleIn(MiniAppSessionScope::class)
internal class ProceduralBlockBlastAudioPlayer @Inject constructor(
    private val audio: MiniAppAudio,
) : BlockBlastAudioPlayer {
    private val sfx = SessionAudioProgram(audio, BlockBlastAudio.program)

    override fun playFeedback(type: FeedbackType) {
        sfx.playSfx(BlockBlastAudio.voice(type))
    }

    override fun playPlace() {
        sfx.playSfx(BlockBlastAudio.Place)
    }

    override fun playClear(lines: Int) {
        if (lines > 0) sfx.playSfx(BlockBlastAudio.ClearPop)
    }

    override fun playGameOver() {
        sfx.playSfx(BlockBlastAudio.GameOver)
    }

    override fun playRevive() {
        sfx.playSfx(BlockBlastAudio.Revive)
    }

    override fun playNewBest() {
        sfx.playSfx(BlockBlastAudio.NewBest)
    }
}
