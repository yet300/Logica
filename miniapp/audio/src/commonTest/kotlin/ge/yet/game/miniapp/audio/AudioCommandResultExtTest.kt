package ge.yet.game.miniapp.audio

import kotlin.test.Test

class AudioCommandResultExtTest {
    @Test
    fun `accepted result is consumed without throwing`() {
        AudioCommandResult.Accepted.consumeSilently()
    }

    @Test
    fun `every rejection reason is consumed without throwing`() {
        AudioCommandRejection.entries.forEach { reason ->
            AudioCommandResult.Rejected(reason).consumeSilently()
        }
    }
}
