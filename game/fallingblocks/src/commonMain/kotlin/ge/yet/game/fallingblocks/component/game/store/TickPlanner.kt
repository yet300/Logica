package ge.yet.game.fallingblocks.component.game.store

internal class TickPlanner(
    private val stepMillis: Int = STEP_MILLIS,
    private val maxFrameGapMillis: Int = MAX_FRAME_GAP_MILLIS,
    private val maxAdvancesPerFrame: Int = MAX_ADVANCES_PER_FRAME,
) {
    var remainderMillis: Int = 0
        private set

    fun consume(elapsedMillis: Int): List<Int> {
        remainderMillis += elapsedMillis.coerceIn(0, maxFrameGapMillis)
        val count = minOf(remainderMillis / stepMillis, maxAdvancesPerFrame)
        if (count == 0) return emptyList()
        remainderMillis -= count * stepMillis
        return List(count) { stepMillis }
    }

    fun reset() {
        remainderMillis = 0
    }

    private companion object {
        const val STEP_MILLIS = 16
        const val MAX_FRAME_GAP_MILLIS = 250
        const val MAX_ADVANCES_PER_FRAME = 3
    }
}
