package ge.yet.game.fallingblocks

interface FallingblocksGameEngine {
    fun reduce(state: FallingblocksGameState, action: FallingblocksGameAction): FallingblocksGameState
}

internal object DefaultFallingblocksGameEngine : FallingblocksGameEngine {
    override fun reduce(
        state: FallingblocksGameState,
        action: FallingblocksGameAction,
    ): FallingblocksGameState = when (action) {
        FallingblocksGameAction.Reset -> FallingblocksGameState()
        FallingblocksGameAction.Tick -> state
    }
}
