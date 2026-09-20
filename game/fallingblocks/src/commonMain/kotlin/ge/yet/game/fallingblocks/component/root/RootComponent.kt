package ge.yet.game.fallingblocks.component.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import ge.yet.game.fallingblocks.domain.engine.DefaultFallingBlocksEngine
import ge.yet.game.fallingblocks.domain.model.FallingBlocksEngine
import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.domain.model.GameAction

interface RootComponent {
    val model: Value<Model>

    fun dispatch(action: GameAction)

    data class Model(
        val state: FallingBlocksState,
    )
}

internal class DefaultRootComponent(
    componentContext: ComponentContext,
    private val engine: FallingBlocksEngine = DefaultFallingBlocksEngine,
    seed: Long = 0,
    runId: Long = 0,
) : RootComponent, ComponentContext by componentContext {
    private val mutableModel = MutableValue(
        RootComponent.Model(state = engine.initial(seed = seed, runId = runId)),
    )
    override val model: Value<RootComponent.Model> = mutableModel

    init { componentContext.lifecycle.doOnDestroy { } }

    override fun dispatch(action: GameAction) {
        mutableModel.update { current ->
            current.copy(state = engine.reduce(current.state, action).state)
        }
    }
}
