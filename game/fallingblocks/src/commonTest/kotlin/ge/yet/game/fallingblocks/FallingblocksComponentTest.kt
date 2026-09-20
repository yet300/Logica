package ge.yet.game.fallingblocks

import ge.yet.game.miniapp.testkit.MiniAppLifecycleHarness
import ge.yet.game.fallingblocks.component.root.DefaultRootComponent
import ge.yet.game.fallingblocks.domain.engine.DefaultFallingBlocksEngine
import ge.yet.game.fallingblocks.domain.model.FallingBlocksEngine
import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.domain.model.GameAction
import ge.yet.game.fallingblocks.domain.model.GameFact
import ge.yet.game.fallingblocks.domain.model.GameTransition
import kotlin.test.Test
import kotlin.test.assertEquals

class FallingblocksComponentTest {
    @Test
    fun `component delegates typed actions to the engine`() {
        val lifecycle = MiniAppLifecycleHarness()
        val component = DefaultRootComponent(
            componentContext = lifecycle.componentContext,
            engine = IncrementingFallingBlocksEngine,
        )

        component.dispatch(GameAction.MoveHorizontal(1))

        assertEquals(1, component.model.value.state.score)
        lifecycle.destroy()
    }
}

private object IncrementingFallingBlocksEngine : FallingBlocksEngine {
    override fun initial(seed: Long, runId: Long): FallingBlocksState =
        DefaultFallingBlocksEngine.initial(seed, runId)

    override fun reduce(
        state: FallingBlocksState,
        action: GameAction,
    ): GameTransition = GameTransition(
        state = state.copy(score = state.score + 1),
        facts = listOf(GameFact.Moved(horizontalCells = 1, downwardCells = 0)),
    )
}
