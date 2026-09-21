package ge.yet.game.fallingblocks.domain.model

data class GameTransition(
    val state: FallingBlocksState,
    val facts: List<GameFact>,
)

sealed interface GameFact {
    data class Moved(
        val horizontalCells: Int,
        val downwardCells: Int,
    ) : GameFact

    data object Rotated : GameFact
    data object Blocked : GameFact
    data class HardDropped(val cells: Int) : GameFact
    data object Locked : GameFact
    data class LinesCleared(
        val rows: List<Int>,
        val perfect: Boolean,
    ) : GameFact {
        init {
            require(rows == rows.distinct().sorted())
            require(rows.all { it in 0 until Board.TOTAL_HEIGHT })
        }

        val count: Int get() = rows.size
    }
    data class LevelChanged(val level: Int) : GameFact
    data object ToppedOut : GameFact
    data object Revived : GameFact
}

interface FallingBlocksEngine {
    fun initial(seed: Long, runId: Long): FallingBlocksState

    fun reduce(state: FallingBlocksState, action: GameAction): GameTransition
}
