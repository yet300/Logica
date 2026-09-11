package ge.yet.game.blockblast.data.audio

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import ge.yet.game.blockblast.domain.model.FeedbackType
import ge.yet.game.domain.repository.AudioRepository
import ge.yet.game.miniapp.metro.MiniAppSessionScope

internal interface BlockBlastAudioPlayer {
    fun playFeedback(type: FeedbackType)
    fun startMusic()
    fun stopMusic()
}

// Session-scoped (not by state — stateless — but by the graph identity test);
// scope lives here because Metro forbids scopes on @Binds declarations.
@SingleIn(MiniAppSessionScope::class)
internal class DefaultBlockBlastAudioPlayer @Inject constructor(
    private val audio: AudioRepository,
) : BlockBlastAudioPlayer {
    override fun playFeedback(type: FeedbackType) {
        audio.playSound(BlockBlastAudioAssets.voice(type))
    }

    override fun startMusic() {
        audio.startMusic(BlockBlastAudioAssets.music)
    }

    override fun stopMusic() {
        audio.stopMusic()
    }
}
