package ge.yet.game.miniapp.audio.presets

import ge.yet.game.miniapp.audio.AudioProgram
import ge.yet.game.miniapp.audio.MiniAppAudio
import ge.yet.game.miniapp.audio.SfxName
import ge.yet.game.miniapp.audio.consumeSilently

/**
 * Idempotent session-bound music starter and program-scoped SFX sender.
 *
 * Games keep their own event-to-sound mapping (which [SfxName] to play and when);
 * this helper owns only the mechanics shared by every session adapter: starting
 * music at most once per session and routing SFX through one [AudioProgram]
 * while silently dropping host-rejected commands. It holds no Metro scope;
 * the game-owned adapter stays `@SingleIn(MiniAppSessionScope::class)`.
 */
class SessionAudioProgram(
    private val audio: MiniAppAudio,
    private val program: AudioProgram,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        audio.playMusic(program).consumeSilently()
    }

    fun playSfx(name: SfxName) {
        audio.playSfx(program, name).consumeSilently()
    }
}
