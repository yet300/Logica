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
    data class LinesCleared(val count: Int, val perfect: Boolean) : GameFact
    data class LevelChanged(val level: Int) : GameFact
    data object ToppedOut : GameFact
    data object Revived : GameFact
}

interface FallingBlocksEngine {
    fun initial(seed: Long, runId: Long): FallingBlocksState

    fun reduce(state: FallingBlocksState, action: GameAction): GameTransition
}
