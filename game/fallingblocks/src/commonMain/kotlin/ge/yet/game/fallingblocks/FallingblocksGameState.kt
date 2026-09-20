package ge.yet.game.fallingblocks

data class FallingblocksGameState(
    val score: Int = 0,
    val isGameOver: Boolean = false,
)

sealed interface FallingblocksGameAction {
    data object Reset : FallingblocksGameAction
    data object Tick : FallingblocksGameAction
}
