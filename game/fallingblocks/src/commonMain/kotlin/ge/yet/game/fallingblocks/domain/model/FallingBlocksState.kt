package ge.yet.game.fallingblocks.domain.model

import ge.yet.game.fallingblocks.domain.engine.BagState

data class FallingBlocksState(
    val board: Board,
    val active: ActivePiece,
    val preview: List<Tetromino>,
    val bag: BagState,
    val score: Long,
    val level: Int,
    val lines: Int,
    val combo: Int,
    val backToBack: Boolean,
    val gravityRemainingMillis: Int,
    val lockRemainingMillis: Int,
    val lockResetCount: Int,
    val lastActionWasRotation: Boolean,
    val revivesUsed: Int,
    val runId: Long,
    val phase: GamePhase,
)

enum class GamePhase {
    PLAYING,
    TERMINAL,
}

sealed interface GameAction {
    data object RotateClockwise : GameAction

    data class MoveHorizontal(val cells: Int) : GameAction

    data class SoftDrop(val cells: Int) : GameAction

    data object HardDrop : GameAction

    data class AdvanceTime(val millis: Int) : GameAction

    data object Revive : GameAction
}
