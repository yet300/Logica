package ge.yet.game.fallingblocks

import ge.yet.game.miniapp.testkit.MiniAppLifecycleHarness
import ge.yet.game.fallingblocks.component.root.DefaultRootComponent
import kotlin.test.Test
import kotlin.test.assertEquals

class FallingblocksComponentTest {
    @Test
    fun `component delegates typed actions to the engine`() {
        val lifecycle = MiniAppLifecycleHarness()
        val component = DefaultRootComponent(
            componentContext = lifecycle.componentContext,
            engine = IncrementingFallingblocksGameEngine,
        )

        component.dispatch(FallingblocksGameAction.Tick)

        assertEquals(1, component.model.value.state.score)
        lifecycle.destroy()
    }
}

private object IncrementingFallingblocksGameEngine : FallingblocksGameEngine {
    override fun reduce(
        state: FallingblocksGameState,
        action: FallingblocksGameAction,
    ): FallingblocksGameState = when (action) {
        FallingblocksGameAction.Reset -> FallingblocksGameState()
        FallingblocksGameAction.Tick -> state.copy(score = state.score + 1)
    }
}
