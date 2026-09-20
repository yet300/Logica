package ge.yet.game.fallingblocks.component.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import ge.yet.game.fallingblocks.FallingblocksGameAction
import ge.yet.game.fallingblocks.FallingblocksGameEngine
import ge.yet.game.fallingblocks.FallingblocksGameState
import ge.yet.game.fallingblocks.DefaultFallingblocksGameEngine

interface RootComponent {
    val model: Value<Model>

    fun dispatch(action: FallingblocksGameAction)

    data class Model(
        val state: FallingblocksGameState = FallingblocksGameState(),
    )
}

internal class DefaultRootComponent(
    componentContext: ComponentContext,
    private val engine: FallingblocksGameEngine = DefaultFallingblocksGameEngine,
) : RootComponent, ComponentContext by componentContext {
    private val mutableModel = MutableValue(RootComponent.Model())
    override val model: Value<RootComponent.Model> = mutableModel

    init { componentContext.lifecycle.doOnDestroy { } }

    override fun dispatch(action: FallingblocksGameAction) {
        mutableModel.update { current ->
            current.copy(state = engine.reduce(current.state, action))
        }
    }
}
