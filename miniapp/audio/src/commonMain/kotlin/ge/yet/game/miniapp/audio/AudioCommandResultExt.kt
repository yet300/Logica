package ge.yet.game.miniapp.audio

/**
 * Best-effort consumption of a procedural-audio command result.
 *
 * Extracted from the identical private `consume()` helpers in the 2048 and
 * Fruit Merge session audio adapters. Rejection is intentional and
 * host-owned (suppression, teardown, full queue), so adapters swallow it
 * instead of branching per reason at every call site.
 */
fun AudioCommandResult.consumeSilently() {
    when (this) {
        AudioCommandResult.Accepted -> Unit
        is AudioCommandResult.Rejected -> when (reason) {
            AudioCommandRejection.INVALID_PROGRAM -> Unit
            AudioCommandRejection.UNKNOWN_SFX -> Unit
            AudioCommandRejection.UNKNOWN_CONTROL -> Unit
            AudioCommandRejection.CONTROL_OUT_OF_RANGE -> Unit
            AudioCommandRejection.PLAYBACK_SUPPRESSED -> Unit
            AudioCommandRejection.SESSION_CLOSED -> Unit
            AudioCommandRejection.COMMAND_QUEUE_FULL -> Unit
            AudioCommandRejection.BACKEND_UNAVAILABLE -> Unit
        }
    }
}
